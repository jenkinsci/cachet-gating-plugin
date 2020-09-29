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

import com.redhat.jenkins.plugins.cachet.GlobalCachetConfiguration;
import com.redhat.jenkins.plugins.cachet.SourceTemplate;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JcascTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("single.yaml")
    public void single() throws Exception {
        GlobalCachetConfiguration g = GlobalCachetConfiguration.get();
        assertEquals("https://example.com", g.getCachetUrl());
        assertFalse(g.isIgnoreSSL());
        assertNull(g.getLabel());
    }

    @Test
    @ConfiguredWithCode("multiple.yaml")
    public void multiple() {

        GlobalCachetConfiguration g = GlobalCachetConfiguration.get();
        assertEquals("https://example.com/bax", g.getCachetUrl());
        assertEquals("bax", g.getLabel());

        Map<String, SourceTemplate> sources = g.getSources().stream().collect(Collectors.toMap(SourceTemplate::getLabel, s -> s));

        assertEquals("https://example.com/foo", sources.get("foo").getCachetUrl());
        assertEquals("foo", sources.get("foo").getLabel());
        assertFalse(sources.get("foo").isIgnoreSSL());

        assertEquals("https://example.com/bar", sources.get("bar").getCachetUrl());
        assertEquals("bar", sources.get("bar").getLabel());
        assertTrue(sources.get("bar").isIgnoreSSL());

        assertEquals("https://example.com/baz", sources.get("baz").getCachetUrl());
        assertEquals("baz", sources.get("baz").getLabel());
        assertFalse(sources.get("baz").isIgnoreSSL());
    }
}
