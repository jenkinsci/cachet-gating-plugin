package com.redhat.jenkins.plugins.cachet;

import hudson.model.InvisibleAction;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.util.HashMap;
import java.util.Map;

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


public class CachetGatingAction extends InvisibleAction {

    @Whitelisted
    public Map<String, CachetGatingMetrics> getGatingMetricsMap() {
        if (gatingMetricsMap == null) {
            gatingMetricsMap = new HashMap<String, CachetGatingMetrics>();
        }
        return gatingMetricsMap;
    }

    public void setGatingMetricsMap(Map<String, CachetGatingMetrics> gatingMetricsMap) {
        this.gatingMetricsMap = gatingMetricsMap;
    }

    public CachetGatingAction(Map<String, CachetGatingMetrics> gatingMetricsMap) {
        this.gatingMetricsMap = gatingMetricsMap;
    }

    public CachetGatingAction() {
        this.gatingMetricsMap = new HashMap<String, CachetGatingMetrics>();
    }

    @Override
    public String toString() {
        return "CachetGatingAction{" +
                "gatingMetricsMap=" + gatingMetricsMap +
                '}';
    }

    private Map<String, CachetGatingMetrics> gatingMetricsMap;
}
