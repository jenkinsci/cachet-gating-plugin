package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.ExtensionList;

import javax.annotation.Nonnull;

import hudson.model.Failure;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.*;
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

@Extension
public final class GlobalCachetConfiguration extends GlobalConfiguration {

    private List<SourceTemplate> sources = new ArrayList<>();

    @DataBoundConstructor
    public GlobalCachetConfiguration() {
        load();
    }

    @DataBoundSetter
    public void setSources(List<SourceTemplate> sources) {
        this.sources = sources;
    }

    public List<SourceTemplate> getSources() {
        return sources != null ? sources : Collections.emptyList();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return Messages.pluginName();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        HashMap<String, String> urls = new HashMap<>();
        HashMap<String, String> labels = new HashMap<>();
        Object obj = json.get("sources");
        if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            for (Object obj2 : arr) {
                JSONObject providerObj = (JSONObject) obj2;
                String url = providerObj.getString("cachetUrl");
                String label = providerObj.getString("label");
                if (urls.containsKey(url)) {
                    throw new Failure("Attempt adding duplicate Cachet urls - " + url);
                }
                if (labels.containsKey(label)) {
                    throw new Failure("Attempt adding duplicate labels - " + label);
                }
                urls.put(url, url);
                labels.put(label, label);
            }
        }
        req.bindJSON(this, json);
        save();
        return true;
    }

    public static @Nonnull GlobalCachetConfiguration get() {
        ExtensionList<GlobalCachetConfiguration> extensions = ExtensionList.lookup(GlobalCachetConfiguration.class);
        assert extensions.size() == 1: "One cachet configuration expected, got " + extensions.size();
        return extensions.get(0);
    }
}
