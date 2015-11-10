/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.telephony.ua;

import static java.lang.Integer.parseInt;
import static javax.servlet.sip.SipServlet.OUTBOUND_INTERFACES;
import static javax.servlet.sip.SipServletResponse.SC_OK;
import static javax.servlet.sip.SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.toHex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.telephony.UserRegistration;
import org.mobicents.servlet.restcomm.util.DigestAuthentication;

import com.telestax.servlet.MonitoringService;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
public final class UserAgentManager extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private boolean authenticateUsers = true;
    private final SipFactory factory;
    private final DaoManager storage;
    private final ServletContext servletContext;
    private ActorRef monitoringService;

    public UserAgentManager(final Configuration configuration, final SipFactory factory, final DaoManager storage,
            final ServletContext servletContext) {
        super();
        // this.configuration = configuration;
        this.servletContext = servletContext;
        monitoringService = (ActorRef) servletContext.getAttribute(MonitoringService.class.getName());
        final Configuration runtime = configuration.subset("runtime-settings");
        this.authenticateUsers = runtime.getBoolean("authenticate");
        this.factory = factory;
        this.storage = storage;
        final ActorContext context = context();
        context.setReceiveTimeout(Duration.create(60, TimeUnit.SECONDS));
    }

    private void clean() {
        final RegistrationsDao registrations = storage.getRegistrationsDao();
        final List<Registration> results = registrations.getRegistrations();
        for (final Registration result : results) {
            final DateTime expires = result.getDateExpires();
            if (expires.isBeforeNow() || expires.isEqualNow()) {
                registrations.removeRegistration(result);
            }
        }
    }

    private String header(final String nonce, final String realm, final String scheme) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(scheme).append(" ");
        buffer.append("realm=\"").append(realm).append("\", ");
        buffer.append("nonce=\"").append(nonce).append("\"");
        return buffer.toString();
    }

    private void authenticate(final Object message) throws IOException {
        final SipServletRequest request = (SipServletRequest) message;
        final SipServletResponse response = request.createResponse(SC_PROXY_AUTHENTICATION_REQUIRED);
        final String nonce = nonce();
        final SipURI uri = (SipURI) request.getTo().getURI();
        final String realm = uri.getHost();
        final String header = header(nonce, realm, "Digest");
        response.addHeader("Proxy-Authenticate", header);
        response.send();
    }

    private void keepAlive() throws Exception {
        final RegistrationsDao registrations = storage.getRegistrationsDao();
        final List<Registration> results = registrations.getRegistrations();
        for (final Registration result : results) {
            final String to = result.getLocation();
            ping(to);
        }
    }

    private String nonce() {
        final byte[] uuid = UUID.randomUUID().toString().getBytes();
        final char[] hex = toHex(uuid);
        return new String(hex).substring(0, 31);
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof ReceiveTimeout) {
            clean();
            keepAlive();
        } else if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("REGISTER".equalsIgnoreCase(method)) {
                if(authenticateUsers) { // https://github.com/Mobicents/RestComm/issues/29 Allow disabling of SIP authentication
                    final String authorization = request.getHeader("Proxy-Authorization");
                    if (authorization != null && permitted(authorization, method)) {
                        register(message);
                    } else {
                        authenticate(message);
                    }
                } else {
                    register(message);
                }
            }
        } else if (message instanceof SipServletResponse) {
            pong(message);
        }
    }

    private void patch(final SipURI uri, final String address, final int port) throws UnknownHostException {
        final InetAddress host = InetAddress.getByName(uri.getHost());
        final String ip = host.getHostAddress();
        uri.setHost(address);
        uri.setPort(port);
    }

    private boolean permitted(final String authorization, final String method) {
        final Map<String, String> map = toMap(authorization);
        final String user = map.get("username");
        final String algorithm = map.get("algorithm");
        final String realm = map.get("realm");
        final String uri = map.get("uri");
        final String nonce = map.get("nonce");
        final String nc = map.get("nc");
        final String cnonce = map.get("cnonce");
        final String qop = map.get("qop");
        final String response = map.get("response");
        final ClientsDao clients = storage.getClientsDao();
        final Client client = clients.getClient(user);
        if (client != null && Client.ENABLED == client.getStatus()) {
            final String password = client.getPassword();
            final String result = DigestAuthentication.response(algorithm, user, realm, password, nonce, nc, cnonce, method,
                    uri, null, qop);
            return result.equals(response);
        } else {
            return false;
        }
    }

    private void ping(final String to) throws Exception {
        final SipApplicationSession application = factory.createApplicationSession();
        String toTransport = ((SipURI) factory.createURI(to)).getTransportParam();
        if (toTransport == null) {
            // RESTCOMM-301 NPE in RestComm Ping
            toTransport = "udp";
        }
        if (toTransport.equalsIgnoreCase("ws") || toTransport.equalsIgnoreCase("wss")) {
            return;
        }
        final SipURI outboundInterface = outboundInterface(toTransport);
        StringBuilder buffer = new StringBuilder();
        buffer.append("sip:restcomm").append("@").append(outboundInterface.getHost());
        final String from = buffer.toString();
        final SipServletRequest ping = factory.createRequest(application, "OPTIONS", from, to);
        final SipURI uri = (SipURI) factory.createURI(to);
        ping.pushRoute(uri);
        ping.setRequestURI(uri);
        final SipSession session = ping.getSession();
        session.setHandler("UserAgentManager");
        ping.send();
    }

    private void pong(final Object message) {
        final SipServletResponse response = (SipServletResponse) message;
        if (response.getMethod().equalsIgnoreCase("OPTIONS")){
            // if(response.getSession().isValid()) {
            // response.getSession().invalidate();
            // }
            String user = ((SipURI)response.getTo().getURI()).getUser();
            String host = ((SipURI)response.getTo().getURI()).getHost();

            final RegistrationsDao registrations = storage.getRegistrationsDao();
            final List<Registration> registration = registrations.getRegistrations(user);
            for (Registration reg : registration) {
                if (reg.getLocation().equalsIgnoreCase(host))
                    registrations.removeRegistration(reg);
            }

            if (response.getApplicationSession().isValid()) {
                response.getApplicationSession().invalidate();
            }
        }
    }

    private SipURI outboundInterface(String toTransport) {
        SipURI result = null;
        @SuppressWarnings("unchecked")
        final List<SipURI> uris = (List<SipURI>) servletContext.getAttribute(OUTBOUND_INTERFACES);
        for (final SipURI uri : uris) {
            final String transport = uri.getTransportParam();
            if (toTransport != null && toTransport.equalsIgnoreCase(transport)) {
                result = uri;
            }
        }
        return result;
    }

    private void register(final Object message) throws Exception {
        final SipServletRequest request = (SipServletRequest) message;
        final Address contact = request.getAddressHeader("Contact");
        // Get the expiration time.
        int ttl = contact.getExpires();
        if (ttl == -1) {
            final String expires = request.getHeader("Expires");
            if (expires != null) {
                ttl = parseInt(expires);
            } else {
                ttl = 3600;
            }
        }
        // Make sure registrations don't last more than 1 hour.
        if (ttl > 3600) {
            ttl = 3600;
        }
        // Get the rest of the information needed for a registration record.
        String name = contact.getDisplayName();
        String ua = request.getHeader("User-Agent");
        final SipURI to = (SipURI) request.getTo().getURI();
        final String aor = to.toString();
        final String user = to.getUser();
        final SipURI uri = (SipURI) contact.getURI();
        final String ip = request.getInitialRemoteAddr();
        final int port = request.getInitialRemotePort();
        final String transport = uri.getTransportParam();

        //Issue 306: https://telestax.atlassian.net/browse/RESTCOMM-306
        final String initialIpBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemoteAddr");
        final String initialPortBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemotePort");
        if(initialIpBeforeLB != null && !initialIpBeforeLB.isEmpty() && initialPortBeforeLB != null && !initialPortBeforeLB.isEmpty()) {
            logger.info("Client in front of LB. Patching URI: "+uri.toString()+" with IP: "+initialIpBeforeLB+" and PORT: "+initialPortBeforeLB+" for USER: "+user);
            patch(uri, initialIpBeforeLB, Integer.valueOf(initialPortBeforeLB));
        } else {
            logger.info("Patching URI: " + uri.toString() + " with IP: " + ip + " and PORT: " + port + " for USER: " + user);
            patch(uri, ip, port);
        }

        final StringBuffer buffer = new StringBuffer();
        buffer.append("sip:").append(user).append("@").append(uri.getHost()).append(":").append(uri.getPort());
        // https://bitbucket.org/telestax/telscale-restcomm/issue/142/restcomm-support-for-other-transports-than
        if (transport != null) {
            buffer.append(";transport=").append(transport);
        }
        final String address = buffer.toString();
        // Prepare the response.
        final SipServletResponse response = request.createResponse(SC_OK);
        // Update the data store.
        final Sid sid = Sid.generate(Sid.Type.REGISTRATION);
        final DateTime now = DateTime.now();

        // Issue 87
        // (http://www.google.com/url?q=https://bitbucket.org/telestax/telscale-restcomm/issue/87/verb-and-not-working-for-end-to-end-calls%23comment-5855486&usd=2&usg=ALhdy2_mIt4FU4Yb_EL-s0GZCpBG9BB8eQ)
        // if display name or UA are null, the hasRegistration returns 0 even if there is a registration
        if (name == null)
            name = user;
        if (ua == null)
            ua = "GenericUA";

        boolean webRTC = isWebRTC(transport, ua);

        final Registration registration = new Registration(sid, now, now, aor, name, user, ua, ttl, address, webRTC);
        final RegistrationsDao registrations = storage.getRegistrationsDao();

        if (ttl == 0) {
            // Remove Registration if ttl=0
            registrations.removeRegistration(registration);
            response.setHeader("Expires", "0");
            monitoringService.tell(new UserRegistration(user, address, false), self());
            logger.info("The user agent manager unregistered " + user + " at address "+address);
        } else {
            monitoringService.tell(new UserRegistration(user, address, true), self());
            if (registrations.hasRegistration(registration)) {
                // Update Registration if exists
                registrations.updateRegistration(registration);
                logger.info("The user agent manager updated " + user + " at address " + address);
            } else {
                // Add registration since it doesn't exists on the DB
                registrations.addRegistration(registration);
                logger.info("The user agent manager registered " + user + " at address " + address);
            }
            response.setHeader("Contact", contact(uri, ttl));
        }
        // Success
        response.send();
        // Cleanup
        // if(request.getSession().isValid()) {
        // request.getSession().invalidate();
        // }
        if (request.getApplicationSession().isValid()) {
            try {
                request.getApplicationSession().invalidate();
            } catch (IllegalStateException exception) {
            }
        }
    }

    /**
     * Checks whether the client is WebRTC or not.
     *
     * <p>
     * A client is considered WebRTC if one of the following statements is true:<br>
     * 1. The chosen transport is WebSockets (transport=ws).<br>
     * 2. The chosen transport is WebSockets Secured (transport=wss).<br>
     * 3. The User-Agent corresponds to one of TeleStax mobile clients.
     * </p>
     *
     * @param transport
     * @param userAgent
     * @return
     */
    private boolean isWebRTC(String transport, String userAgent) {
//        return "ws".equals(transport) || "wss".equals(transport) || userAgent.contains("Restcomm");
        return false;
    }

    private String contact(final SipURI uri, final int expires) {
        final Address contact = factory.createAddress(uri);
        contact.setExpires(expires);
        return contact.toString();
    }

    private Map<String, String> toMap(final String header) {
        final Map<String, String> map = new HashMap<String, String>();
        final int endOfScheme = header.indexOf(" ");
        map.put("scheme", header.substring(0, endOfScheme).trim());
        final String[] tokens = header.substring(endOfScheme + 1).split(",");
        for (final String token : tokens) {
            final String[] values = token.trim().split("=");
            map.put(values[0].toLowerCase(), values[1].replace("\"", ""));
        }
        return map;
    }
}
