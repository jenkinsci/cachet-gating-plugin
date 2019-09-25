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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.jenkins.plugins.cachet.GlobalCachetConfiguration;
import com.redhat.jenkins.plugins.cachet.ResourceProvider;
import com.redhat.jenkins.plugins.cachet.ResourceUpdater;
import com.redhat.jenkins.plugins.cachet.SourceTemplate;
import hudson.Util;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
public class CachetGatingPluginSSLTest {
    private static final int SERVICE_SSL_PORT = 32443;

    private static final String TEST_CACHE_CONTEXT = "cachet";
    private static final String TEST_CACHE_URL = "https://localhost:" + SERVICE_SSL_PORT + "/" + TEST_CACHE_CONTEXT;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().httpsPort(SERVICE_SSL_PORT));

    @SuppressWarnings("unused")
    private WireMock wireMock;

    @Before
    public void setup() {
        wireMock = new WireMock(SERVICE_SSL_PORT);
        stubFor(get(urlMatching("/" + TEST_CACHE_CONTEXT + ".+")).willReturn(ok("resources.txt")));
    }

    /**
     * Utility method for reading files.
     * @param path path to file.
     * @return contents of file.
     */
    private static String readFile(String path) {
        try {
            URL res = CachetGatingPluginSSLTest.class.getResource(path);
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
        SourceTemplate sourceTemplate = new SourceTemplate(TEST_CACHE_URL, "", true);
        ResourceProvider.SINGLETON.setResourcesForTests(ResourceUpdater.getResources(sourceTemplate));

        List<String> names = ResourceProvider.SINGLETON.getResourceNames();

        assert names != null;
        assertEquals(names.size(), 11);
        assertEquals(names.toString(), "[brew, ci-rhos, covscan, dummy, errata, gerrit.host.prod.eng.bos.redhat.com, polarion, rdo-cloud, rpmdiff, umb, zabbix-sysops]");
    }

    @Test
    public void testGetResourceNamesSSLBroken() {
        SourceTemplate sourceTemplate = new SourceTemplate(TEST_CACHE_URL, "", false);
        ResourceProvider.SINGLETON.setResourcesForTests(ResourceUpdater.getResources(sourceTemplate));

        List<String> names = ResourceProvider.SINGLETON.getResourceNames();
        assertNull(names);
    }
}
