package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.log4j.Logger;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */

@RunWith(Arquillian.class)
public class AccountsEndpointTest {
    private final static Logger logger = Logger.getLogger(AccountsEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    static boolean accountUpdated = false;
    static boolean accountCreated = false;

    private String adminUsername = "administrator@company.com";
    private String adminFriendlyName = "Default Administrator Account";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String newAdminPassword = "mynewpassword";
    private String newAdminAuthToken = "8e70383c69f7a3b7ea3f71b02f3e9731";
    private String userEmailAddress = "gvagenas@restcomm.org";
    private String userPassword = "1234";
    private String unprivilegedSid = "AC00000000000000000000000000000000";
    private String unprivilegedUsername = "unprivileged@company.com";
    private String unprivilegedAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String organizationSid = "ORec3515ebea5243b6bde0444d84b05b80";


//    @Before
//    public void before() {
//        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
//                adminUsername, newAdminPassword, adminAccountSid, null);
//    }

    @After
    public void after() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test
    public void testGetAccount() {
        // Get Account using admin email address and user email address
        JsonObject adminAccount = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminUsername);
        assertTrue(adminAccount.get("sid").getAsString().equals(adminAccountSid));

    }

    @Test
    public void testGetAccountByFriendlyName() {
        // Try to get Account using admin friendly name and user email address
        int code = 0;
        try {
            JsonObject adminAccount = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                    adminFriendlyName, adminAuthToken, adminUsername);
        } catch (UniformInterfaceException e) {
            code = e.getResponse().getStatus();
        }
        // Logins using friendly name are not allowed anymore
        assertTrue(code == 401);
    }

    @Test
    public void testCreateAccount() {
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminUsername, newAdminPassword, adminAccountSid, null, null);
        accountUpdated = true;
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, userEmailAddress, userPassword, organizationSid);
        accountCreated = true;
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, userEmailAddress);

        String usernameHashed = "AC" + (new Md5Hash(userEmailAddress).toString());
        assertTrue(createAccountResponse.get("sid").getAsString().equals(usernameHashed));

        assertTrue(createAccountResponse.get("auth_token").equals(getAccountResponse.get("auth_token")));
        String userPasswordHashed = new Md5Hash(userPassword).toString();
        assertTrue(getAccountResponse.get("auth_token").getAsString().equals(userPasswordHashed));
        assertTrue(getAccountResponse.get("organization_sid").getAsString().equals(organizationSid));
    }

    @Test
    public void testCreateAdministratorAccount() {
        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null, null);
        }
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, "administrator@company.com", "1234", organizationSid);
        assertNull(createAccountResponse);
    }

    @Test
    public void testCreateAccountTwice() {
        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null, null);
        }
        if (!accountCreated) {
            JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                    adminUsername, newAdminAuthToken, userEmailAddress, userPassword, organizationSid);
            accountCreated = true;
            assertNotNull(createAccountResponse);
        }
        JsonObject createAccountResponseSecondTime = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, userEmailAddress, userPassword, organizationSid);
        assertNull(createAccountResponseSecondTime);
    }

    @Test
    public void testGetAccounts() throws InterruptedException {
        if (!accountUpdated){
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null, null);
        }

        if (!accountCreated) {
            // Create account
            RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername, newAdminAuthToken,
                    userEmailAddress, userPassword, organizationSid);
        }
        // Get Account using admin email address and user email address
        JsonObject account1 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, userEmailAddress);
        // Get Account using admin account sid and user sid
        JsonObject account2 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminAccountSid,
                newAdminAuthToken, account1.get("sid").getAsString());

        assertTrue(account1.toString().equals(account2.toString()));

    }

    /**
     * Test access without administrator type of access. Instead a common role and permissions is used.
     */
    @Test
    public void testAccountAcccessUsingRoles() {
        ClientResponse response = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(),unprivilegedUsername, unprivilegedAuthToken, unprivilegedSid);
        Assert.assertEquals(200,response.getStatus());
    }

    @Deployment(name = "ClientsEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_accounts_test", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
