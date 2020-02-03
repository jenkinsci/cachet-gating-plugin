package com.redhat.jenkins.plugins.integration;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Sets;

import com.redhat.jenkins.plugins.cachet.CachetGatingAction;
import com.redhat.jenkins.plugins.cachet.CachetGatingMetrics;
import com.redhat.jenkins.plugins.cachet.CachetJobProperty;
import com.redhat.jenkins.plugins.cachet.GlobalCachetConfiguration;
import com.redhat.jenkins.plugins.cachet.ResourceProvider;
import com.redhat.jenkins.plugins.cachet.ResourceUpdater;
import com.redhat.jenkins.plugins.cachet.SourceTemplate;
import com.redhat.jenkins.plugins.cachet.matrix.CachetAxis;

import hudson.Util;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.queue.QueueTaskFuture;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CachetMatrixTest {
    private static final int SERVICE_PORT = 32001;

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

    private SourceTemplate sourceTemplate = new SourceTemplate(TEST_CACHE_URL, "", false);


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
        ResourceProvider.SINGLETON.setResourcesForTests(ResourceUpdater.getResources(sourceTemplate));
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

    private MatrixProject createMatrixProject() throws IOException {
        MatrixProject p = j.createProject(MatrixProject.class);

        AxisList axes = new AxisList();
        axes.add(new CachetAxis("cachet1", Arrays.asList("brew", "umb")));
        axes.add(new CachetAxis("cachet2", Arrays.asList("brew", "errata")));
        axes.add(new TextAxis("db","mysql","oracle"));
        p.setAxes(axes);

        return p;
    }

    private Set<List<String>> getCachetCombos(){
        return Sets.newHashSet(Collections.singletonList("brew"),
                Arrays.asList("brew", "errata"), Arrays.asList("umb", "brew"), Arrays.asList("umb", "errata"));
    }

    @Test
    public void testNumberOfAxis() throws IOException {
        MatrixProject p = createMatrixProject();

        assertEquals(p.getAxes().size(), 3);
        assertEquals(p.getAxes().subList(CachetAxis.class).size(), 2);
    }

    @Test
    public void testRunningMatrix() throws Exception {
        MatrixProject p = createMatrixProject();
        stubFor(brewOutage);
        ResourceProvider.SINGLETON.setResourcesForTests(ResourceUpdater.getResources(sourceTemplate));

        QueueTaskFuture<MatrixBuild> futureTask = p.scheduleBuild2(0);
        futureTask.waitForStart();

        Thread.sleep(10000);

        stubFor(normal);
        ResourceProvider.SINGLETON.setResourcesForTests(ResourceUpdater.getResources(sourceTemplate));

        MatrixBuild b1 = futureTask.get();
        List<MatrixRun> runs = b1.getExactRuns();
        assertEquals(runs.size(), 8);

        Set<List<String>> cachetCombos = getCachetCombos();

        for (MatrixRun matrixRun : runs) {
            CachetJobProperty cachetJobProperty = matrixRun.getParent().getProperty(CachetJobProperty.class);
            assertNotNull(cachetJobProperty);
            assertTrue(cachetCombos.contains(cachetJobProperty.getResources()));

            CachetGatingAction gatingAction = matrixRun.getAction(CachetGatingAction.class);
            Map<String, CachetGatingMetrics> metricsMap = gatingAction.getGatingMetricsMap();
            assertNotNull(metricsMap);

            CachetGatingMetrics gatingMetricsBrew = metricsMap.get("brew");
            CachetGatingMetrics gatingMetricsUmb = metricsMap.get("umb");
            CachetGatingMetrics gatingMetricsErrata = metricsMap.get("errata");

            if (gatingMetricsBrew != null)
                assertTrue(gatingMetricsBrew.getGatedTimeElapsed() > 0);
            else {
                assertEquals(0, (long) gatingMetricsUmb.getGatedTimeElapsed());
                assertEquals(0, (long) gatingMetricsErrata.getGatedTimeElapsed());
            }

            j.assertBuildStatusSuccess(matrixRun);
        }
    }
}
