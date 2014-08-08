package org.mobicents.servlet.restcomm.provisioning.number.vi;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */

@RunWith(Arquillian.class)
public class AvailablePhoneNumbersEndpointTest {
    private final static Logger logger = Logger.getLogger(AvailablePhoneNumbersEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    static boolean accountUpdated = false;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String baseURL = "2012-04-24/Accounts/" + adminAccountSid + "/AvailablePhoneNumbers/";
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080
    
    /*
     * https://www.twilio.com/docs/api/rest/available-phone-numbers#local-get-basic-example-1
     * available local phone numbers in the United States in the 510 area code.
     */
    @Test
    public void testSearchUSLocalPhoneNumbersWith501AreaCode() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("getDIDs"))
                .withRequestBody(containing("501"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(AvailablePhoneNumbersEndpointTestUtils.body501AreaCode)));
        // Get Account using admin email address and user email address
    	Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "US/Local.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        ClientResponse clientResponse = webResource.queryParam("AreaCode","501").accept("application/json")
                .get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonResponse);
        
        assertTrue(jsonResponse.size() == 32);
        System.out.println((jsonResponse.get(0).getAsJsonObject().toString()));
        assertTrue(jsonResponse.get(0).getAsJsonObject().toString().equalsIgnoreCase(AvailablePhoneNumbersEndpointTestUtils.firstJSonResult501AreaCode));
    }

    /*
     * https://www.twilio.com/docs/api/rest/available-phone-numbers#local-get-basic-example-2
     * Find local phone numbers in the United States starting with 510555.
     */
    @Test
    public void testSearchUSLocalPhoneNumbersWithPattern() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("getDIDs"))
                .withRequestBody(containing("501"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(AvailablePhoneNumbersEndpointTestUtils.body501AreaCode)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "US/Local.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        ClientResponse clientResponse = webResource.queryParam("Contains","501555****").accept("application/json")
                .get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonResponse);
        
        assertTrue(jsonResponse.size() == 2);
        System.out.println((jsonResponse.get(0).getAsJsonObject().toString()));
        assertTrue(jsonResponse.get(0).getAsJsonObject().toString().equalsIgnoreCase(AvailablePhoneNumbersEndpointTestUtils.firstJSonResult501ContainsPattern));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/available-phone-numbers#local-get-basic-example-3
     * Find local phone numbers that match the pattern 'STORM'.
     */
    @Test
    public void testSearchUSLocalPhoneNumbersWithLetterPattern() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("getDIDs"))
                .withRequestBody(containing("675"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(AvailablePhoneNumbersEndpointTestUtils.body501AreaCode)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "US/Local.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        ClientResponse clientResponse = webResource.queryParam("Contains","STORM").accept("application/json")
                .get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonResponse);
        
        assertTrue(jsonResponse.size() == 1);
        System.out.println((jsonResponse.get(0).getAsJsonObject().toString()));
        assertTrue(jsonResponse.get(0).getAsJsonObject().toString().equalsIgnoreCase(AvailablePhoneNumbersEndpointTestUtils.firstJSonResult501ContainsLetterPattern));
    }

    
    @Deployment(name = "AvailablePhoneNumbersEndpointTest", managed = true, testable = false)
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
