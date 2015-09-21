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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.AccountList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.AccountConverter;
import org.mobicents.servlet.restcomm.http.converter.AccountListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakClient.KeycloakClientException;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class AccountsEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected  IdentityConfigurator identityConfigurator;
    protected Gson gson;
    protected XStream xstream;

    public AccountsEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        identityConfigurator = (IdentityConfigurator) context.getAttribute(IdentityConfigurator.class.getName());
        final AccountConverter converter = new AccountConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Account.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new AccountListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    // Create an account out of data. The sid for the new account is random and the account is considered a child of parentAccountSid
    private Account createFrom(final Sid parentAccountSid, final MultivaluedMap<String, String> data) {
        validate(data);

        final DateTime now = DateTime.now();
        final String emailAddress = data.getFirst("EmailAddress");

        // Issue 108: https://bitbucket.org/telestax/telscale-restcomm/issue/108/account-sid-could-be-a-hash-of-the
        //final Sid sid = Sid.generate(Sid.Type.ACCOUNT, emailAddress);
        // Decoupling email from account sid as of https://github.com/Mobicents/RestComm/issues/257
        final Sid sid = Sid.generate(Sid.Type.ACCOUNT);

        String friendlyName = emailAddress;
        if (data.containsKey("FriendlyName")) {
            friendlyName = data.getFirst("FriendlyName");
        }
        final Account.Type type = Account.Type.FULL;
        Account.Status status = Account.Status.ACTIVE;
        if (data.containsKey("Status")) {
            status = Account.Status.valueOf(data.getFirst("Status"));
        }
        //final String password = data.getFirst("Password");
        //final String authToken = new Md5Hash(password).toString();
        final String role = data.getFirst("Role");
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(null)).append("/Accounts/").append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        return new Account(sid, now, now, emailAddress, friendlyName, parentAccountSid, type, status, null, role, uri);
    }

    /**
     * Creates an account object from the details contained in an AccessToken. No db persistence is done here.
     * @param token
     * @return
     */
    private Account accountFromAccessToken(AccessToken token) {

        final DateTime now = DateTime.now();
        final String username = token.getPreferredUsername();

        // Issue 108: https://bitbucket.org/telestax/telscale-restcomm/issue/108/account-sid-could-be-a-hash-of-the
        final Sid sid = Sid.generate(Sid.Type.ACCOUNT, username );

        // Use keycloak firstname/lastname as a friendly name if available. Otherwise fall back to keycloak username.
        String friendlyName = token.getName(); // e.g. Orestis Tsakiridis
        String emailAddress = token.getPreferredUsername(); // TODO what do we do here? Restcomm treats emailAddress as username

        final Account.Type type = Account.Type.FULL;
        Account.Status status = Account.Status.ACTIVE;

        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(null)).append("/Accounts/").append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        //return new Account(sid, now, now, emailAddress, friendlyName, accountSid, type, status, authToken, role, uri);
        return new Account(sid, now, now, emailAddress, friendlyName, null, type, status, "notused", "KeycloakUser", uri);
    }

    /**
     * If the requested account does not exist and it's the user himself that makes the request create a new one based
     * on the token information. Of course take into account the configuration too.
     */
    private Account handleMissingAccount( String accountSid, AccessToken token) {
        Account account = null;
        if ( identityConfigurator.getAutoImportUsers() ) {
            if ( token.getPreferredUsername().equals(accountSid) ) {
                account = accountFromAccessToken(token);
                accountsDao.addAccount(account);
                logger.info("Automatically imported user '" + accountSid + "' as Restcomm account '" + account.getSid() + "'" );
            }
        }
        return account;
    }

    protected Response getAccount(final String accountSid, final MediaType responseType) {
        try {
            // now load the account that is operated upon
            Sid sid = null;
            Account account = null;
            // Is the account identifier used a sid like "AC5699ea39a9a8c86820ea241e843c221e" ?
            if (Sid.pattern.matcher(accountSid).matches()) {
                try {
                    sid = new Sid(accountSid);
                    account = accountsDao.getAccount(sid);
                } catch (Exception e) {
                    return status(NOT_FOUND).build();
                }

            } else { // If not, try to load by FriendlyName, email etc.
                try {
                    account = accountsDao.getAccount(accountSid);
                    if ( account == null )
                        account = handleMissingAccount(accountSid, getKeycloakAccessToken());
                    sid = account.getSid();
                } catch (Exception e) {
                    return status(NOT_FOUND).build();
                }
            }

            if (account == null) {
                return status(NOT_FOUND).build();
            } else {
                // make sure the logged user can access this account
                secure(account, "RestComm:Read:Accounts");
                if (APPLICATION_XML_TYPE == responseType) {
                    final RestCommResponse response = new RestCommResponse(account);
                    return ok(xstream.toXML(response), APPLICATION_XML).build();
                } else if (APPLICATION_JSON_TYPE == responseType) {
                    return ok(gson.toJson(account), APPLICATION_JSON).build();
                } else {
                    return null;
                }
            }
        } catch (UnauthorizedException e) {
            return status(UNAUTHORIZED).build();
        }
    }

    protected Response deleteAccount(final String sid) {
        final Subject subject = SecurityUtils.getSubject();
        final Sid accountSid = new Sid((String) subject.getPrincipal());
        final Sid sidToBeRemoved = new Sid(sid);

        try {
            Account account = accountsDao.getAccount(sidToBeRemoved);
            secure(account, "RestComm:Delete:Accounts");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        // Prevent removal of Administrator account
        if (sid.equalsIgnoreCase(accountSid.toString()))
            return status(BAD_REQUEST).build();

        if (accountsDao.getAccount(sidToBeRemoved) == null)
            return status(NOT_FOUND).build();

        accountsDao.removeAccount(sidToBeRemoved);
        return ok().build();
    }

    // Returns (sub-)accounts for logged user. Proper sub-account implementation is still an issue to implement - https://github.com/Mobicents/RestComm/issues/227
    protected Response getAccounts(final MediaType responseType) {
        try {
            secure("RestComm:Read:Accounts");
        } catch(final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        // get the account for the logged user (Decouples email from account SID. We rely on the username/email. We DON'T produce the account sid from username  and use the sid to retrieve the account.
        //final Account account = accountsDao.getAccount(username);
        final Account account = identityContext.getEffectiveAccount(); // uses either oauth token or APIKey specified account
        if (account == null) {
            return status(NOT_FOUND).build();
        } else {
            final List<Account> accounts = new ArrayList<Account>();
            accounts.add(account); // TODO maybe we need to remove this at some point. There is a different API call for retrieving the users own account
            accounts.addAll(accountsDao.getAccounts(account.getSid())); // remember getSid() retrieves the accounts own id while getAccountSid() retrieves the sid of the parent account
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(new AccountList(accounts));
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(accounts), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response putAccount(final MultivaluedMap<String, String> data, final MediaType responseType) {
        try {
            secure("RestComm:Create:Accounts");
        } catch(final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        final Account parentAccount = identityContext.getEffectiveAccount();
        if ( parentAccount == null )
            return status(NOT_FOUND).build();
        Account account = null; // the newly created account
        try {
            account = createFrom(parentAccount.getSid(), data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        // check if the account that is being created already exists
        if ( accountsDao.getAccount(account.getEmailAddress()) != null )
            return status(CONFLICT).build();

        String childRole = getChildRole(parentAccount);
        //account.setRole( childRole ); - no point in setting roles in restcomm accounts since they are not used

        // Automatic keycloak user creation is disabled for now:
        // Everything seems set. Let's try creating the user in keycloak
        /*try {
            String password = data.getFirst("Password"); // password is already encoded to auth_token in createFrom() so we need to extract it again
            createKeycloakUser(account,password);
        } catch (KeycloakClientException e) {
            if ( e.getHttpStatusCode() == 409 )
                return status(CONFLICT).build();
        }
        */

        // assign a random authToken for the account
        account = account.setAuthToken(generateApiKey());

        // now, store it in Restcomm too
        accountsDao.addAccount(account);
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(account), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(account);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private Account update(final Account account, final MultivaluedMap<String, String> data) {
        Account result = account;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("Status")) {
            result = result.setStatus(Account.Status.getValueOf(data.getFirst("Status")));
        } else {
            result = result.setStatus(Account.Status.ACTIVE);
        }
        // Password is deprecated as a concept
        /*if (data.containsKey("Password")) {
            final String hash = new Md5Hash(data.getFirst("Password")).toString();
            result = result.setAuthToken(hash);
        }
        // Auth token is no longer specified by the user
        if (data.containsKey("Auth_Token")) {
            result = result.setAuthToken(data.getFirst("Auth_Token"));
        }*/
        return result;
    }

    // converts submitted form data to a keyclock UserRepresentation object
    /*
    private static UserRepresentation toUserRepresentation(MultivaluedMap<String, String> userData) {
        UserRepresentation user = new UserRepresentation();
        user.setFirstName("test");
        return user;
    }
    */

    /*
    private static UserRepresentation toUserRepresentation(Account account) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(account.getEmailAddress());
        user.setFirstName(account.getFriendlyName());
        user.setEnabled(account.getStatus() == Account.Status.ACTIVE);
        return user;
    } */

    protected Response updateAccount(final String accountSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);

        if (account == null) {
            return status(NOT_FOUND).build();
        } else {
            // make sure the logged user can modify accounts AND that he owns (or is a parent of) this account
            secure(account, "RestComm:Modify:Accounts");
            // update the model
            account = update(account, data);
            try {
                updateKeycloakUser(account);
                // if all goes well, persist the updated account in database
                accountsDao.updateAccount(account);

                if (APPLICATION_JSON_TYPE == responseType) {
                    return ok(gson.toJson(account), APPLICATION_JSON).build();
                } else if (APPLICATION_XML_TYPE == responseType) {
                    final RestCommResponse response = new RestCommResponse(account);
                    return ok(xstream.toXML(response), APPLICATION_XML).build();
                } else {
                    return null;
                }
            } catch (KeycloakClientException e) {
                if ( e.getHttpStatusCode() != null ) {
                    // TODO maybe return an different HTTP status code according to the value returned from the keycloak server
                    logger.error(e,e);
                    return status(INTERNAL_SERVER_ERROR).build();
                } else
                    throw new RuntimeException();

            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    private void updateKeycloakUser(Account restcommAccount) throws IOException, KeycloakClientException {
        // retrieve the user from keycloak server, update and store back again
        logger.warn("updateKeycloakUser(): this method is deprecated");
        /*
        UserRepresentation keycloakUser = keycloakClient.getUserInfo(restcommAccount.getEmailAddress());
        keycloakUser.setFirstName(restcommAccount.getFriendlyName());
        keycloakUser.setEnabled( (restcommAccount.getStatus() == Account.Status.ACTIVE) ) ;
        keycloakClient.updateUser(restcommAccount.getEmailAddress(), keycloakUser);
        */
    }

    private void createKeycloakUser(Account restcommAccount, String password) throws KeycloakClientException {
        logger.warn("updateKeycloakUser(): this method is deprecated");
        /*
        // create and populate keycloak user object
        UserRepresentation user = new UserRepresentation();
        user.setUsername(restcommAccount.getEmailAddress());
        user.setFirstName(restcommAccount.getFriendlyName());
        user.setEnabled(restcommAccount.getStatus() == Account.Status.ACTIVE);
        // submit for storage
        keycloakClient.createUser(restcommAccount.getEmailAddress(), user);
        keycloakClient.resetUserPassword(restcommAccount.getEmailAddress(), password, false);
        // Add default roles
        List<String> roles = new ArrayList<String>();
        roles.add("RestcommUser");
        roles.add("Developer");
        keycloakClient.setUserRoles(restcommAccount.getEmailAddress(), roles);
        */
    }

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        if (!data.containsKey("EmailAddress")) {
            throw new NullPointerException("Email address can not be null.");
        } /*else if (!data.containsKey("Password")) {
            throw new NullPointerException("Password can not be null.");
        }*/
    }

    // calculates the role for an account based on parent account and logged user. For now it always create a Developer
    private String getChildRole(Account parentAccount ) {
        // TODO add a proper implementation
        return "Developer";
    }

    // generates a random api key to be used for accessing an Account using Basic HTTP authentication
    private String generateApiKey() {
        // TODO generate proper API key values. Not just UUIDs
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
