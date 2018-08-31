package com.redhat.jenkins.plugins.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import hudson.Util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.HttpStatus;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.jenkins.plugins.cachet.ResourceUpdater;
import com.redhat.jenkins.plugins.integration.po.GlobalCachetConfiguration;

/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
@WithPlugins({"cachet-gating-plugin"})
public class CachetGatingPluginTest extends AbstractJUnitTest {
    private static final int SERVICE_PORT = 32000;
    private static final int SC_NOT_AUTHORIZED = 401;

    private static final String TEST_CACHE_CONTEXT = "cachet";
    private static final String TEST_CACHE_URL = "http://localhost:" + SERVICE_PORT + "/" + TEST_CACHE_CONTEXT;

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(SERVICE_PORT);

    private WireMock wireMock;

    @Before
    public void setup() {
        wireMock = new WireMock(SERVICE_PORT);
        jenkins.configure();
        GlobalCachetConfiguration pluginConfig = new GlobalCachetConfiguration(jenkins.getConfigPage());
        pluginConfig.url(TEST_CACHE_URL);
    }
    /**
     * Utility method for reading files.
     * @param path path to file.
     * @return contents of file.
     */
    public static String readFile(String path) {
        try {
            URL res = CachetGatingPluginTest.class.getResource(path);
            return Util.loadFile(
                    new File(res.toURI()),
                    Charset.forName("UTF-8")
            );
        } catch (IOException e) {
            throw new Error(e);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    private ResponseDefinitionBuilder ok(String path, String... args) {
        String body = String.format(readFile(path), (Object[])args);
        return aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader("Content-Type", "text/json")
                .withBody(body);
    }

    private ResponseDefinitionBuilder notfound() {
        return aResponse().withStatus(HttpStatus.SC_NOT_FOUND);
    }

    private ResponseDefinitionBuilder notauthorized() {
        return aResponse().withStatus(SC_NOT_AUTHORIZED).withBody("Not Authorized");
    }

    private ResponseDefinitionBuilder error(String path, int statusCode, String... args) {
        String body = String.format(readFile(path), (Object[])args);
        return aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "text/json")
                .withBody(body);
    }

    @Test
    public void testMe() {
        stubFor(get(urlMatching(TEST_CACHE_CONTEXT + ".+"))
                .willReturn(error("resources.txt", HttpStatus.SC_OK)));
        ResourceUpdater.setResources();
    }

//    @Test(expected = MBSException.class)
//    public void testMissingattribute() {
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(error("missingattribute.txt", 400)));
//        submitRequest();
//    }
//
//    @Test(expected = MBSException.class)
//    public void testHashnotfound() {
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(error("hashnotfound.txt", 422)));
//        submitRequest();
//    }
//
//    @Test(expected = MBSException.class)
//    public void testAlreadyExists() {
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(error("alreadyexists.txt", 409)));
//        submitRequest();
//    }
//
//    @Test(expected = MBSException.class)
//    public void testCommitnotinbranch() {
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(error("commitnotinbranch.txt", 400)));
//        submitRequest();
//    }
//
//    @Test
//    public void testSubmit() throws MBSException {
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("submitted.txt")));
//        SubmittedRequest request = submitRequest();
//        assertNotNull(request);
//        System.out.println(request.getName());
//        assertTrue(request.getName().equals("testmodule"));
//        assertTrue(request.getState() == 1);
//        assertTrue(request.getTasks().getRpms().getRpmList().containsKey("ed"));
//        assertNotNull(request.getScmurl());
//    }
//
//    @Test
//    public void testRequestWaiting() throws MBSException {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("waiting.txt")));
//        QueryResult result = query();
//        assertNotNull(result);
//        assertNotNull(result.getItems());
//        assertTrue(result.getItems().size() == 1);
//        assertTrue(result.getItems().get(0).getState() == 1);
//        assertNull(result.getItems().get(0).getTimeCompleted());
//    }
//
//    @Test
//    public void testRequestReady() throws MBSException {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("ready.txt")));
//        QueryResult result = query();
//        assertNotNull(result);
//        assertNotNull(result.getItems());
//        assertTrue(result.getItems().size() == 1);
//        assertTrue(result.getItems().get(0).getState() == 5);
//        assertNotNull(result.getItems().get(0).getTimeCompleted());
//        assertTrue(result.getItems().get(0).getTasks().getRpms().getRpmList().containsKey("ed"));
//        assertTrue(result.getItems().get(0).getTasks().getRpms().getRpmList().containsKey("module-build-macros"));
//    }
//
//    @Test
//    public void testNoBuildsInQueue() throws MBSException {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("nobuilds.txt")));
//        QueryResult result = query();
//        assertNotNull(result);
//        assertNotNull(result.getItems());
//        assertTrue(result.getItems().size() == 0);
//    }
//
//    protected String loadPipelineScript(String name) {
//        try {
//            return new String(IOUtils.toByteArray(getClass().getResourceAsStream(name)));
//        } catch (Throwable t) {
//            throw new RuntimeException("Could not read resource:[" + name + "].");
//        }
//    }
//
//    private WorkflowRun simpleSubmit(String passwordToOffer) throws Exception {
//        String credId   = "bobs-password";
//
//        StringCredentialsImpl c = new StringCredentialsImpl(CredentialsScope.GLOBAL,
//                credId, credId, Secret.fromString(passwordToOffer));
//        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), c);
//
//        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "simpleSubmit");
//        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simpleSubmit.groovy"), false));
//        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
//        assertNotNull(b);
//        jenkins.waitForCompletion(b);
//        List<String> log = b.getLog(1000);
//        for (String s: log) {
//            System.out.println(s);
//        }
//        return b;
//    }
//
//    @Test(expected = MBSException.class)
//    public void queryNotFound() throws Exception {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(notfound()));
//        query();
//    }
//
//
//    @Test(expected = MBSException.class)
//    public void queryModuleNotFound() throws Exception {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(notfound()));
//        queryModule();
//    }
//
//    @Test
//    public void simpleSubmit() throws Exception {
//        String username = "bob";
//        String password = "s3cr3t";
//
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .withBasicAuth(username, password)
//                .willReturn(ok("submitted.txt")));
//
//        WorkflowRun b = simpleSubmit(password);
//        jenkins.assertBuildStatusSuccess(b);
//        jenkins.assertLogContains("my submission id is: 1", b);
//    }
//
//    @Test
//    public void simpleSubmitBadPassword() throws Exception {
//        String username = "bob";
//        String password = "badpass";
//
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .withBasicAuth(username, password)
//                .willReturn(notauthorized()));
//
//        WorkflowRun b = simpleSubmit(password);
//        jenkins.assertBuildStatus(Result.FAILURE, b);
//        jenkins.assertLogContains("Call to http://localhost:32000/module-build-service/1/module-builds/?verbose=true returned 401 Response was: Not Authorized", b);
//    }
//
//    @Test
//    public void simpleQuery() throws Exception {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("ready.txt")));
//        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "simpleQuery");
//        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simpleQuery.groovy"), false));
//        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
//        assertNotNull(b);
//        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(b));
//        List<String> log = b.getLog(1000);
//        for (String s: log) {
//            System.out.println(s);
//        }
//        jenkins.assertLogContains("Module id: 1 is ready", b);
//    }
//
//    @Test
//    public void simpleModuleQuery() throws Exception {
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("module-ready.txt")));
//        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "simpleModuleQuery");
//        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simpleModuleQuery.groovy"), false));
//        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
//        assertNotNull(b);
//        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(b));
//        List<String> log = b.getLog(1000);
//        for (String s: log) {
//            System.out.println(s);
//        }
//        jenkins.assertLogContains("Module id: 1 is ready", b);
//    }
//
//    @Test
//    public void complete() throws Exception {
//        String credId   = "bobs-password";
//        String username = "bob";
//        String password = "s3cr3t";
//
//        stubFor(post(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .withBasicAuth(username, password)
//                .willReturn(ok("submitted.txt")));
//
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("module-waiting.txt")));
//
//        StringCredentialsImpl c = new StringCredentialsImpl(CredentialsScope.GLOBAL,
//                credId, credId, Secret.fromString(password));
//        CredentialsProvider.lookupStores(jenkins).iterator().next().addCredentials(Domain.global(), c);
//
//        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "complete");
//        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("complete.groovy"), false));
//        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
//        assertNotNull(b);
//        Thread.sleep(10000);
//        stubFor(get(urlMatching(MBSUtils.MBS_URLPREFIX + ".+"))
//                .willReturn(ok("module-ready.txt")));
//        jenkins.waitForCompletion(b);
//        List<String> log = b.getLog(1000);
//        for (String s: log) {
//            System.out.println(s);
//        }
//        jenkins.assertBuildStatusSuccess(b);
//        jenkins.assertLogContains("my submission id is: 1", b);
//        jenkins.assertLogContains("Module id: 1 is ready", b);
//    }
//
//    public static TaskListener getTaskListenerMock() {
//        TaskListener mockTaskListener = mock(TaskListener.class);
//        when(mockTaskListener.getLogger()).thenReturn(System.out);
//        return mockTaskListener;
//    }

}
