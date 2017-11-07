package org.restcomm.connect.testsuite.telephony;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Map;

import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommConferenceParticipantsTool;
import org.restcomm.connect.testsuite.http.RestcommConferenceTool;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by gvagenas on 5/19/16.
 */
@RunWith(Arquillian.class)
public class DialConferenceTerminationTest {
    private final static Logger logger = Logger.getLogger(DialConferenceTerminationTest.class.getName());

    private static final String version = Version.getVersion();
    private static final byte[] bytes = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
    private static final String body = new String(bytes);

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool3;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    private String dialRestcomm = "sip:1111@127.0.0.1:5080";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialConferenceTool1");
        tool3 = new SipStackTool("DialConferenceTool3");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        georgeSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    private int getActiveConferencesSize() {
        JsonObject conferences = RestcommConferenceTool.getInstance().getConferences(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        JsonArray conferenceArray = conferences.getAsJsonArray("conferences");
        int activeConfSize = 0;
        for(int i = 0; i < conferenceArray.size(); i++) {
            JsonObject confObj = conferenceArray.get(i).getAsJsonObject();
            String confStatus = confObj.get("status").getAsString();
            logger.info("confStatus: "+confStatus);
            if (confStatus.matches("RUNNING.*")) {
            	activeConfSize++;
            }
        }
        return activeConfSize;
    }

    private int getParticipantsSize(final String sid) {
        JsonObject participants = RestcommConferenceParticipantsTool.getInstance().getParticipants(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        JsonArray participantsArray = participants.getAsJsonArray("calls");
        return participantsArray.size();
    }

    private String getConferenceSid(final String name) {
        JsonObject conferences = RestcommConferenceTool.getInstance().getConferences(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        JsonArray conferenceArray = conferences.getAsJsonArray("conferences");
        String confSid = null;
        for(int i = 0; i < conferenceArray.size(); i++) {
            JsonObject confObj = conferenceArray.get(i).getAsJsonObject();
            String confName = confObj.get("friendly_name").getAsString();
            String confStatus = confObj.get("status").getAsString();
            logger.info("confStatus: "+confStatus);
            if (confName.equalsIgnoreCase(name) && confStatus.matches("RUNNING.*")) {
                confSid = confObj.get("sid").getAsString();
                return confSid;
            }
        }
        return null;
    }


    private final String confRoom1 = "confRoom1";
    private String dialConfernceRcml = "<Response><Dial><Conference>"+confRoom1+"</Conference></Dial></Response>";
    @Test
    public synchronized void testDialConferenceTimeoutTest() throws InterruptedException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialConfernceRcml)));

        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        int responseGeorge = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseGeorge == Response.TRYING || responseGeorge == Response.RINGING);

        if (responseGeorge == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());
        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        String conferenceSid = getConferenceSid(confRoom1);
        assertEquals(2, getParticipantsSize(conferenceSid));
        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertEquals(2, liveCalls);
        assertEquals(2, liveCallsArraySize);

        JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        Map<String, Integer> mgcpResources = MonitoringServiceTool.getInstance().getMgcpResources(metrics);
        int mgcpEndpoints = mgcpResources.get("MgcpEndpoints");
        int mgcpConnections = mgcpResources.get("MgcpConnections");

        assertTrue(mgcpEndpoints>0);
        assertTrue(mgcpConnections>0);

        // Wait for the media to play and the call to hangup.
        bobCall.listenForDisconnect();
        georgeCall.listenForDisconnect();

        assertTrue(bobCall.waitForDisconnect(80 * 1000));
        assertTrue(georgeCall.waitForDisconnect(80 * 1000));

        Thread.sleep(1000);
        assertEquals(0, getActiveConferencesSize());
        assertEquals(0, getParticipantsSize(conferenceSid));
        liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertEquals(0, liveCalls);
        assertEquals(0, liveCallsArraySize);

        metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        mgcpResources = MonitoringServiceTool.getInstance().getMgcpResources(metrics);
        mgcpEndpoints = mgcpResources.get("MgcpEndpoints");
        mgcpConnections = mgcpResources.get("MgcpConnections");

        assertEquals(0, mgcpEndpoints);
        assertEquals(0, mgcpConnections);
    }

    @Test
    public synchronized void testDialConferenceTerminateConferenceViaAPI() throws Exception {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialConfernceRcml)));

        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        int responseGeorge = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseGeorge == Response.TRYING || responseGeorge == Response.RINGING);

        if (responseGeorge == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());
        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertEquals(1, getActiveConferencesSize());
        String conferenceSid = getConferenceSid(confRoom1);
        assertEquals(2, getParticipantsSize(conferenceSid));
        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertEquals(2, liveCalls);
        assertEquals(2, liveCallsArraySize);

        JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        Map<String, Integer> mgcpResources = MonitoringServiceTool.getInstance().getMgcpResources(metrics);
        int mgcpEndpoints = mgcpResources.get("MgcpEndpoints");
        int mgcpConnections = mgcpResources.get("MgcpConnections");

        assertTrue(mgcpEndpoints>0);
        assertTrue(mgcpConnections>0);
        
        //terminate the conference
        RestcommConferenceTool.getInstance().modifyConference(deploymentUrl.toString(),adminAccountSid, adminAuthToken, conferenceSid, "Completed");

        // Wait for the media to play and the call to hangup.
        bobCall.listenForDisconnect();
        georgeCall.listenForDisconnect();

        assertTrue(bobCall.waitForDisconnect(20 * 1000));
        assertTrue(georgeCall.waitForDisconnect(20 * 1000));

        Thread.sleep(1000);

        assertEquals(0, getActiveConferencesSize());
        assertEquals(0, getParticipantsSize(conferenceSid));
        liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertEquals(0, liveCalls);
        assertEquals(0, liveCallsArraySize);

        metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        mgcpResources = MonitoringServiceTool.getInstance().getMgcpResources(metrics);
        mgcpEndpoints = mgcpResources.get("MgcpEndpoints");
        mgcpConnections = mgcpResources.get("MgcpConnections");

        assertEquals(0, mgcpEndpoints);
        assertEquals(0, mgcpConnections);
    }

    @Deployment(name = "DialConferenceTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm-conference.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest_new", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
