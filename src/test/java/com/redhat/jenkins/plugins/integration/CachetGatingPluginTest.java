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
package com.redhat.jenkins.plugins.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redhat.jenkins.plugins.cachet.CachetGatingAction;
import com.redhat.jenkins.plugins.cachet.CachetGatingMetrics;
import com.redhat.jenkins.plugins.cachet.CachetJobProperty;
import hudson.Util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hudson.model.queue.QueueTaskFuture;
import hudson.util.IOUtils;
import org.apache.http.HttpStatus;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.jenkins.plugins.cachet.GlobalCachetConfiguration;
import com.redhat.jenkins.plugins.cachet.Resource;
import com.redhat.jenkins.plugins.cachet.ResourceProvider;
import com.redhat.jenkins.plugins.cachet.ResourceStatus;
import com.redhat.jenkins.plugins.cachet.ResourceUpdater;

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


public class CachetGatingPluginTest {
    private static final int SERVICE_PORT = 32000;

    private static final String TEST_CACHE_CONTEXT = "cachet";
    private static final String TEST_CACHE_URL = "http://localhost:" + SERVICE_PORT + "/" + TEST_CACHE_CONTEXT;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(SERVICE_PORT);

    @SuppressWarnings("unused")
    private WireMock wireMock;
    private UUID mockID = UUID.randomUUID();
    private MappingBuilder normal;
    private MappingBuilder brewOutage;



    @Before
    public void setup() {
        WireMockConfiguration.options()
                // Statically set the HTTP port number. Defaults to 8080.
                .port(SERVICE_PORT).notifier(new ConsoleNotifier(true));

        GlobalCachetConfiguration gcc = GlobalCachetConfiguration.get();
        gcc.setCachetUrl(TEST_CACHE_URL);

        normal = get(urlMatching("/" + TEST_CACHE_CONTEXT + ".+")).withId(mockID).willReturn(ok("resources.txt"));
        brewOutage = get(urlMatching("/" + TEST_CACHE_CONTEXT + ".+")).withId(mockID).willReturn(ok("resources-brew-outage.txt"));

        stubFor(normal);
        ResourceUpdater.setResources();
    }

    /**
     * Utility method for reading files.
     * @param path path to file.
     * @return contents of file.
     */
    private static String readFile(String path) {
        try {
            URL res = CachetGatingPluginTest.class.getResource(path);
            return Util.loadFile(
                    new File(res.toURI()),
                    Charset.forName("UTF-8")
            );
        } catch (IOException | URISyntaxException e) {
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

    @Test
    public void testGetResourceNames() {
        List<String> names = ResourceProvider.SINGLETON.getResourceNames();
        assertEquals(names.size(), 11);
        assertEquals(names.toString(), "[brew, ci-rhos, covscan, dummy, errata, gerrit.host.prod.eng.bos.redhat.com, polarion, rdo-cloud, rpmdiff, umb, zabbix-sysops]");
    }

    @Test
    public void testResourceUp() {
        Resource r = ResourceProvider.SINGLETON.getResource("brew");
        assertNotNull(r);
        assertEquals(r.getName(), "brew");
        assertEquals(r.getStatusId(), ResourceStatus.OPERATIONAL);
    }

    @Test
    public void testResourceDown() {
        Resource r = ResourceProvider.SINGLETON.getResource("rdo-cloud");
        assertNotNull(r);
        assertEquals(r.getName(), "rdo-cloud");
        assertEquals(r.getStatusId(), ResourceStatus.MAJOR_OUTAGE);
    }

    @Test
    public void testGetResources() {
        Map<String, Resource> resources = ResourceProvider.SINGLETON.getResources(Arrays.asList("brew", "rdo-cloud"));
        assertNotNull(resources);
        assertEquals(resources.keySet().size(), 2);
        assertNotNull(resources.get("brew"));
        assertNotNull(resources.get("rdo-cloud"));
    }

    @Test
    public void testGetUnknownResource() {
        Map<String, Resource> resources = ResourceProvider.SINGLETON.getResources(Arrays.asList("garbage"));
        assertNotNull(resources);
        assertEquals(resources.keySet().size(), 1);
        Resource garbage = resources.get("garbage");
        assertNotNull(garbage);
        assertEquals(garbage.getStatusId(), ResourceStatus.UNKNOWN);
    }

    @Test
    public void testGetSomeResources() {
        Map<String, Resource> resources = ResourceProvider.SINGLETON.getResources(Arrays.asList("brew", "garbage"));
        assertNotNull(resources);
        assertEquals(resources.keySet().size(), 2);
        assertNotNull(resources.get("brew"));
        assertNotNull(resources.get("garbage"));
    }

    @Test
    public void triggerBuildNoGating() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "triggerBuildNoGating");
        p.addProperty(new CachetJobProperty(false, true, Arrays.asList("brew")));
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simple.groovy"), false));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        j.assertBuildStatusSuccess(j.waitForCompletion(b));
    }

    @Test
    public void triggerBuildNoGatingStep() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "triggerBuildNoGatingStep");
        p.addProperty(new CachetJobProperty(false,true, Arrays.asList("brew")));
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simple-with-step.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        j.assertBuildStatusSuccess(j.waitForCompletion(b));
    }

    @Test
    public void triggerBuildGating() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "triggerBuildGating");
        p.addProperty(new CachetJobProperty(false, true, Arrays.asList("brew")));
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simple.groovy"), false));
        stubFor(brewOutage);
        ResourceUpdater.setResources();

        QueueTaskFuture<WorkflowRun> item = p.scheduleBuild2(0);
        assertNotNull(item);

        Thread.currentThread().sleep(10000);

        stubFor(normal);
        ResourceUpdater.setResources();

        WorkflowRun b2 = item.get();
        j.assertBuildStatusSuccess(j.waitForCompletion(b2));
        CachetGatingAction bAction2 = b2.getAction(CachetGatingAction.class);
        Map<String, CachetGatingMetrics> metricsMap2 = bAction2.getGatingMetricsMap();
        assertNotNull(metricsMap2);
        CachetGatingMetrics metric2 = metricsMap2.get("brew");
        assertNotNull(metric2);
        assertTrue(metric2.getGatedTimeElapsed() > 0);
        List<String> log = b2.getLog(1000);
        for (String s: log) {
            System.out.println(s);
        }
    }

    @Test
    public void triggerBuildGatingStep() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "triggerBuildGatingStep");
        p.addProperty(new CachetJobProperty(false, true, Arrays.asList("brew")));
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simple-with-step.groovy"), true));
        stubFor(brewOutage);
        ResourceUpdater.setResources();

        QueueTaskFuture<WorkflowRun> item = p.scheduleBuild2(0);
        assertNotNull(item);

        Thread.currentThread().sleep(10000);

        stubFor(normal);
        ResourceUpdater.setResources();

        WorkflowRun b2 = item.get();
        j.assertBuildStatusSuccess(j.waitForCompletion(b2));
        CachetGatingAction bAction2 = b2.getAction(CachetGatingAction.class);
        Map<String, CachetGatingMetrics> metricsMap2 = bAction2.getGatingMetricsMap();
        assertNotNull(metricsMap2);
        CachetGatingMetrics metric2 = metricsMap2.get("brew");
        assertNotNull(metric2);
        assertTrue(metric2.getGatedTimeElapsed() > 0);
        List<String> log = b2.getLog(1000);
        for (String s: log) {
            System.out.println(s);
        }
        j.assertLogContains(ResourceStatus.MAJOR_OUTAGE.toString(), b2);
    }

    @Test
    public void triggerBuildGatingOperationalStep() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "triggerBuildGatingOperationalStep");
        p.addProperty(new CachetJobProperty(false, true, Arrays.asList("errata")));
        p.setDefinition(new CpsFlowDefinition(loadPipelineScript("simple-with-step.groovy"), true));
        stubFor(normal);
        ResourceUpdater.setResources();

        WorkflowRun b2 = j.buildAndAssertSuccess(p);

        List<String> log = b2.getLog(1000);
        for (String s: log) {
            System.out.println(s);
        }
        CachetGatingAction bAction2 = b2.getAction(CachetGatingAction.class);
        Map<String, CachetGatingMetrics> metricsMap2 = bAction2.getGatingMetricsMap();
        assertNotNull(metricsMap2);
        CachetGatingMetrics metric2 = metricsMap2.get("errata");
        assertNotNull(metric2.getGatingStatus());
        j.assertLogContains(ResourceStatus.OPERATIONAL.toString(), b2);
    }

    protected String loadPipelineScript(String name) {
        try {
            return new String(IOUtils.toByteArray(getClass().getResourceAsStream(name)));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }
}
