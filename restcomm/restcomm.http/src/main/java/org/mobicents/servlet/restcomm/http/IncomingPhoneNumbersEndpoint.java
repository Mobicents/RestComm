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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberFilter;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.AvailableCountriesConverter;
import org.mobicents.servlet.restcomm.http.converter.AvailableCountriesList;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberConverter;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.loader.ObjectFactory;
import org.mobicents.servlet.restcomm.loader.ObjectInstantiationException;
import org.mobicents.servlet.restcomm.provisioning.number.api.ContainerConfiguration;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberType;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.NumberParseException.ErrorType;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com
 * @author jean.deruelle@telestax.com
 */
@NotThreadSafe
public abstract class IncomingPhoneNumbersEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected PhoneNumberProvisioningManager phoneNumberProvisioningManager;
    PhoneNumberParameters phoneNumberParameters;
    private IncomingPhoneNumbersDao dao;
    protected AccountsDao accountsDao;
    private XStream xstream;
    protected Gson gson;

    public IncomingPhoneNumbersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() throws ObjectInstantiationException {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        super.init(configuration.subset("runtime-settings"));
        dao = storage.getIncomingPhoneNumbersDao();
        accountsDao = storage.getAccountsDao();

        phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) context.getAttribute("PhoneNumberProvisioningManager");
        if(phoneNumberProvisioningManager == null) {
            final String phoneNumberProvisioningManagerClass = configuration.getString("phone-number-provisioning[@class]");
            Configuration phoneNumberProvisioningConfiguration = configuration.subset("phone-number-provisioning");
            Configuration telestaxProxyConfiguration = configuration.subset("runtime-settings").subset("telestax-proxy");

            phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) new ObjectFactory(getClass().getClassLoader())
                    .getObjectInstance(phoneNumberProvisioningManagerClass);
            ContainerConfiguration containerConfiguration = new ContainerConfiguration(getOutboundInterfaces());
            phoneNumberProvisioningManager.init(phoneNumberProvisioningConfiguration, telestaxProxyConfiguration, containerConfiguration);
            context.setAttribute("phoneNumberProvisioningManager", phoneNumberProvisioningManager);
        }
        Configuration callbackUrlsConfiguration = configuration.subset("phone-number-provisioning").subset("callback-urls");
        phoneNumberParameters = new PhoneNumberParameters(
                callbackUrlsConfiguration.getString("voice[@url]"),
                callbackUrlsConfiguration.getString("voice[@method]"), false,
                callbackUrlsConfiguration.getString("sms[@url]"),
                callbackUrlsConfiguration.getString("sms[@method]"),
                callbackUrlsConfiguration.getString("fax[@url]"),
                callbackUrlsConfiguration.getString("fax[@method]"),
                callbackUrlsConfiguration.getString("ussd[@url]"),
                callbackUrlsConfiguration.getString("ussd[@method]"));

        final IncomingPhoneNumberConverter converter = new IncomingPhoneNumberConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.registerTypeAdapter(IncomingPhoneNumber.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new IncomingPhoneNumberListConverter(configuration));
        xstream.registerConverter(new AvailableCountriesConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    private IncomingPhoneNumber createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        builder.setSid(sid);
        builder.setAccountSid(accountSid);
        String phoneNumber = data.getFirst("PhoneNumber");
        String cost = data.getFirst("Cost");
        builder.setPhoneNumber(phoneNumber);
        builder.setFriendlyName(getFriendlyName(phoneNumber, data));
        if (data.containsKey("VoiceCapable")) {
            builder.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        }
        if (data.containsKey("SmsCapable")) {
            builder.setSmsCapable(Boolean.parseBoolean(data.getFirst("SmsCapable")));
        }
        if (data.containsKey("MmsCapable")) {
            builder.setMmsCapable(Boolean.parseBoolean(data.getFirst("MmsCapable")));
        }
        if (data.containsKey("FaxCapable")) {
            builder.setFaxCapable(Boolean.parseBoolean(data.getFirst("FaxCapable")));
        }
        if (data.containsKey("isSIP")) {
            builder.setPureSip(Boolean.parseBoolean(data.getFirst("isSIP")));
        } else {
            builder.setPureSip(false);
        }
        final String apiVersion = getApiVersion(data);
        builder.setApiVersion(apiVersion);
        builder.setVoiceUrl(getUrl("VoiceUrl", data));
        builder.setVoiceMethod(getMethod("VoiceMethod", data));
        builder.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
        builder.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
        builder.setStatusCallback(getUrl("StatusCallback", data));
        builder.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
        builder.setHasVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
        builder.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        builder.setSmsUrl(getUrl("SmsUrl", data));
        builder.setSmsMethod(getMethod("SmsMethod", data));
        builder.setSmsFallbackUrl(getUrl("SmsFallbackUrl", data));
        builder.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
        builder.setSmsApplicationSid(getSid("SmsApplicationSid", data));

        builder.setUssdUrl(getUrl("UssdUrl", data));
        builder.setUssdMethod(getMethod("UssdMethod", data));
        builder.setUssdFallbackUrl(getUrl("UssdFallbackUrl", data));
        builder.setUssdFallbackMethod(getMethod("UssdFallbackMethod",data));
        builder.setUssdApplicationSid(getSid("UssdApplicationSid",data));

        final Configuration configuration = this.configuration.subset("runtime-settings");
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/IncomingPhoneNumbers/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private String e164(String number) throws NumberParseException {
        final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
        if(!number.startsWith("+")) {
            number = "+" + number;
        }
        final PhoneNumber result = numbersUtil.parse(number, "US");
        if (numbersUtil.isValidNumber(result)) {
            return numbersUtil.format(result, PhoneNumberFormat.E164);
        } else {
            throw new NumberParseException(ErrorType.NOT_A_NUMBER, "This is not a valid number");
        }
    }

    private String getFriendlyName(final String phoneNumber, final MultivaluedMap<String, String> data) {
        String friendlyName = phoneNumber;
        if (data.containsKey("FriendlyName")) {
            friendlyName = data.getFirst("FriendlyName");
        }
        return friendlyName;
    }

    protected Response getIncomingPhoneNumber(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        if (incomingPhoneNumber == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(accountsDao, accountSid, String.valueOf(incomingPhoneNumber.getAccountSid()));
            } catch (AuthorizationException e) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
    }

    protected Response getAvailableCountries(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        List<String> countries = phoneNumberProvisioningManager.getAvailableCountries();
        if(countries == null) {
            countries = new ArrayList<String>();
            countries.add("US");
        }
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(countries), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new AvailableCountriesList(countries));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response getIncomingPhoneNumbers(final String accountSid, final String phoneNumberFilter, final String friendlyNameFilter,
            PhoneNumberType phoneNumberType, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        IncomingPhoneNumberFilter incomingPhoneNumberFilter = new IncomingPhoneNumberFilter(accountSid, friendlyNameFilter, phoneNumberFilter);
        final List<IncomingPhoneNumber> incomingPhoneNumbers = dao.getIncomingPhoneNumbersByFilter(incomingPhoneNumberFilter);

        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(incomingPhoneNumbers), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new IncomingPhoneNumberList(incomingPhoneNumbers));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response putIncomingPhoneNumber(final String accountSid, final MultivaluedMap<String, String> data,
            PhoneNumberType phoneNumberType, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:IncomingPhoneNumbers");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        String number = data.getFirst("PhoneNumber");
        String isSIP = data.getFirst("isSIP");
        // cater to SIP numbers
        if(isSIP == null) {
            try {
                number = e164(number);
            } catch (NumberParseException e) {}
        }
        IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(number);
        if (incomingPhoneNumber == null) {
            incomingPhoneNumber = createFrom(new Sid(accountSid), data);
            phoneNumberParameters.setPhoneNumberType(phoneNumberType);

            org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber phoneNumber = convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber);
            boolean hasSuceeded = false;
            if(phoneNumberProvisioningManager != null && isSIP == null) {
                hasSuceeded = phoneNumberProvisioningManager.buyNumber(phoneNumber, phoneNumberParameters);
            } else if (isSIP != null) {
                hasSuceeded = true;
            }
            if(hasSuceeded) {
                if(phoneNumber.getFriendlyName() != null) {
                    incomingPhoneNumber.setFriendlyName(phoneNumber.getFriendlyName());
                }
                if(phoneNumber.getPhoneNumber() != null) {
                    incomingPhoneNumber.setPhoneNumber(phoneNumber.getPhoneNumber());
                }
                dao.addIncomingPhoneNumber(incomingPhoneNumber);
                if (APPLICATION_JSON_TYPE == responseType) {
                    return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
                } else if (APPLICATION_XML_TYPE == responseType) {
                    final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
                    return ok(xstream.toXML(response), APPLICATION_XML).build();
                }
            }
        }
        return status(BAD_REQUEST).entity("21452").build();
    }

    public Response updateIncomingPhoneNumber(final String accountSid, final String sid,
            final MultivaluedMap<String, String> data, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        try {
            secureLevelControl(accountsDao, accountSid, String.valueOf(incomingPhoneNumber.getAccountSid()));
        } catch (AuthorizationException e) {
            return status(UNAUTHORIZED).build();
        }
        boolean updated = true;
        if(phoneNumberProvisioningManager != null && (incomingPhoneNumber.isPureSip() == null || !incomingPhoneNumber.isPureSip())) {
            updated = phoneNumberProvisioningManager.updateNumber(convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber), phoneNumberParameters);
        }
        if(updated) {
            dao.updateIncomingPhoneNumber(update(incomingPhoneNumber, data));
            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
        return status(BAD_REQUEST).entity("21452").build();
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("PhoneNumber") && !data.containsKey("AreaCode")) {
            throw new NullPointerException("Phone number can not be null.");
        }
    }

    private IncomingPhoneNumber update(final IncomingPhoneNumber incomingPhoneNumber, final MultivaluedMap<String, String> data) {
        if (data.containsKey("ApiVersion")) {
            incomingPhoneNumber.setApiVersion(getApiVersion(data));
        }
        if (data.containsKey("FriendlyName")) {
            incomingPhoneNumber.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("VoiceUrl")) {
            // for all values that qualify as 'empty' populate property with null
            URI uri = getUrl("VoiceUrl", data);
            incomingPhoneNumber.setVoiceUrl(isEmpty(uri.toString()) ? null : uri);
        }
        if (data.containsKey("VoiceMethod")) {
            incomingPhoneNumber.setVoiceMethod(getMethod("VoiceMethod", data));
        }
        if (data.containsKey("VoiceFallbackUrl")) {
            URI uri = getUrl("VoiceFallbackUrl", data);
            incomingPhoneNumber.setVoiceFallbackUrl( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("VoiceFallbackMethod")) {
            incomingPhoneNumber.setVoiceFallbackMethod( getMethod("VoiceFallbackMethod", data) );
        }
        if (data.containsKey("StatusCallback")) {
            URI uri = getUrl("StatusCallback", data);
            incomingPhoneNumber.setStatusCallback( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("StatusCallbackMethod")) {
            incomingPhoneNumber.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
        }
        if (data.containsKey("VoiceCallerIdLookup")) {
            incomingPhoneNumber.setHasVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
        }
        if (data.containsKey("VoiceApplicationSid")) {
            if ( org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("VoiceApplicationSid") ) )
                incomingPhoneNumber.setVoiceApplicationSid(null);
            else
                incomingPhoneNumber.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        }
        if (data.containsKey("SmsUrl")) {
            URI uri = getUrl("SmsUrl", data);
            incomingPhoneNumber.setSmsUrl( isEmpty(uri.toString()) ? null : uri);
        }
        if (data.containsKey("SmsMethod")) {
            incomingPhoneNumber.setSmsMethod(getMethod("SmsMethod", data));
        }
        if (data.containsKey("SmsFallbackUrl")) {
            URI uri = getUrl("SmsFallbackUrl", data);
            incomingPhoneNumber.setSmsFallbackUrl( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("SmsFallbackMethod")) {
            incomingPhoneNumber.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
        }
        if (data.containsKey("SmsApplicationSid")) {
            if ( org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("SmsApplicationSid") ) )
                incomingPhoneNumber.setSmsApplicationSid(null);
            else
                incomingPhoneNumber.setSmsApplicationSid(getSid("SmsApplicationSid", data));

        }

        if (data.containsKey("VoiceCapable")) {
            incomingPhoneNumber.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        }

        if (data.containsKey("SmsCapable")) {
            incomingPhoneNumber.setSmsCapable(Boolean.parseBoolean(data.getFirst("SmsCapable")));
        }

        if (data.containsKey("MmsCapable")) {
            incomingPhoneNumber.setMmsCapable(Boolean.parseBoolean(data.getFirst("MmsCapable")));
        }

        if (data.containsKey("FaxCapable")) {
            incomingPhoneNumber.setFaxCapable(Boolean.parseBoolean(data.getFirst("FaxCapable")));
        }

        if (data.containsKey("UssdUrl")) {
            URI uri = getUrl("UssdUrl", data);
            incomingPhoneNumber.setUssdUrl(isEmpty(uri.toString()) ? null : uri);
        }

        if (data.containsKey("UssdMethod")) {
            incomingPhoneNumber.setUssdMethod(getMethod("UssdMethod", data));
        }

        if (data.containsKey("UssdFallbackUrl")) {
            URI uri = getUrl("UssdFallbackUrl", data);
            incomingPhoneNumber.setUssdFallbackUrl(isEmpty(uri.toString()) ? null : uri);
        }

        if (data.containsKey("UssdFallbackMethod")) {
            incomingPhoneNumber.setUssdFallbackMethod(getMethod("UssdFallbackMethod", data));
        }

        if (data.containsKey("UssdApplicationSid")) {
            if (org.apache.commons.lang.StringUtils.isEmpty(data.getFirst("UssdApplicationSid")))
                incomingPhoneNumber.setUssdApplicationSid(null);
            else
                incomingPhoneNumber.setUssdApplicationSid(getSid("UssdApplicationSid", data));
        }

        incomingPhoneNumber.setDateUpdated(DateTime.now());
        return incomingPhoneNumber;
    }

    public Response deleteIncomingPhoneNumber(final String accountSid, final String sid) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Delete:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        try {
            secureLevelControl(accountsDao, accountSid, String.valueOf(incomingPhoneNumber.getAccountSid()));
        } catch (AuthorizationException e) {
            return status(UNAUTHORIZED).build();
        }
        if(phoneNumberProvisioningManager != null && (incomingPhoneNumber.isPureSip() == null || !incomingPhoneNumber.isPureSip())) {
            phoneNumberProvisioningManager.cancelNumber(convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber));
        }
        dao.removeIncomingPhoneNumber(new Sid(sid));
        return noContent().build();
    }

    @SuppressWarnings("unchecked")
    private List<SipURI> getOutboundInterfaces() {
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(SipServlet.OUTBOUND_INTERFACES);
        return uris;
    }

    protected org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber convertIncomingPhoneNumbertoPhoneNumber(IncomingPhoneNumber incomingPhoneNumber) {
        return new org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber(
                incomingPhoneNumber.getFriendlyName(),
                incomingPhoneNumber.getPhoneNumber(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                incomingPhoneNumber.isVoiceCapable(),
                incomingPhoneNumber.isSmsCapable(),
                incomingPhoneNumber.isMmsCapable(),
                incomingPhoneNumber.isFaxCapable(),
                false);
    }
}
