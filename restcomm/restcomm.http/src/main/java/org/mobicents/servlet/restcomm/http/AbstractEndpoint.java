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
package org.mobicents.servlet.restcomm.http;

import java.net.URI;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@NotThreadSafe
public abstract class AbstractEndpoint {
    protected Logger logger = Logger.getLogger(AbstractEndpoint.class);
    private String defaultApiVersion;
    protected Configuration configuration;
    protected String baseRecordingsPath;
    @Context
    protected ServletContext context;
    @Context
    HttpServletRequest request;

    public AbstractEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        final String path = configuration.getString("recordings-path");
        baseRecordingsPath = StringUtils.addSuffixIfNotPresent(path, "/");
        defaultApiVersion = configuration.getString("api-version");
    }

    protected String getApiVersion(final MultivaluedMap<String, String> data) {
        String apiVersion = defaultApiVersion;
        if (data != null && data.containsKey("ApiVersion")) {
            apiVersion = data.getFirst("ApiVersion");
        }
        return apiVersion;
    }

    protected PhoneNumber getPhoneNumber(final MultivaluedMap<String, String> data) {
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneNumberUtil.parse(data.getFirst("PhoneNumber"), "US");
        } catch (final NumberParseException ignored) {
        }
        return phoneNumber;
    }

    protected String getMethod(final String name, final MultivaluedMap<String, String> data) {
        String method = "POST";
        if (data.containsKey(name)) {
            method = data.getFirst(name);
        }
        return method;
    }

    protected Sid getSid(final String name, final MultivaluedMap<String, String> data) {
        Sid sid = null;
        if (data.containsKey(name)) {
            sid = new Sid(data.getFirst(name));
        }
        return sid;
    }

    protected URI getUrl(final String name, final MultivaluedMap<String, String> data) {
        URI uri = null;
        if (data.containsKey(name)) {
            uri = URI.create(data.getFirst(name));
        }
        return uri;
    }

    protected boolean getHasVoiceCallerIdLookup(final MultivaluedMap<String, String> data) {
        boolean hasVoiceCallerIdLookup = false;
        if (data.containsKey("VoiceCallerIdLookup")) {
            final String value = data.getFirst("VoiceCallerIdLookup");
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return hasVoiceCallerIdLookup;
    }

/*
    TODO compatibility  check for multitenenancy
    protected void secure(final Account account, final String permission) throws AuthorizationException {
        final Subject subject = SecurityUtils.getSubject();
        if (account != null && account.getSid() != null) {
            final Sid accountSid = account.getSid();
            if (account.getStatus().equals(Account.Status.ACTIVE)
                    && (subject.hasRole("Administrator") || (subject.getPrincipal().equals(accountSid) && subject
                            .isPermitted(permission)))) {
                return;
            } else {
                throw new AuthorizationException();
            }
        } else {
            throw new AuthorizationException();
        }
    }

    protected void secureLevelControl(AccountsDao accountsDao, String accountSid, String referenceAccountSid) {
        String sidPrincipal = String.valueOf(SecurityUtils.getSubject().getPrincipal());
        if (!sidPrincipal.equals(accountSid)) {
            Account account = accountsDao.getAccount(new Sid(accountSid));
            if (!sidPrincipal.equals(String.valueOf(account.getAccountSid()))) {
                throw new AuthorizationException();
            } else if (referenceAccountSid != null && !accountSid.equals(referenceAccountSid)) {
                throw new AuthorizationException();
            }
        } else if (referenceAccountSid != null && !sidPrincipal.equals(referenceAccountSid)) {
            throw new AuthorizationException();
        }
    }
*/

    // A general purpose method to test incoming parameters for meaningful data
    protected boolean isEmpty(Object value) {
        if (value == null)
            return true;
        if ( value.equals("") )
            return true;
        return false;
    }

    protected Response toResponse(Outcome outcome) {
        return Response.status(Outcome.toHttpStatus(outcome)).build();
    }
}