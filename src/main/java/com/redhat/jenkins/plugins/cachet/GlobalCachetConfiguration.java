package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.ExtensionList;

import javax.annotation.Nonnull;

import hudson.model.Failure;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
@Symbol("cachetGating")
public final class GlobalCachetConfiguration extends GlobalConfiguration {
    private String cachetUrl;
    private String label;
    private boolean ignoreSSL;

    private List<SourceTemplate> sources = new ArrayList<>();

    @DataBoundConstructor
    public GlobalCachetConfiguration() {
        load();
    }

    public String getCachetUrl() {
        return cachetUrl;
    }

    @DataBoundSetter
    public void setCachetUrl(String cachetUrl) {
        this.cachetUrl = cachetUrl;
    }

    public String getLabel() {
        return label;
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isIgnoreSSL() {
        return ignoreSSL;
    }

    @DataBoundSetter
    public void setIgnoreSSL(boolean ignoreSSL) {
        this.ignoreSSL = ignoreSSL;
    }

    public List<SourceTemplate> getSources() {
        return sources != null ? sources : Collections.emptyList();
    }

    @DataBoundSetter
    public void setSources(List<SourceTemplate> sources) {
        this.sources = sources;
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

        String mainUrl = json.get("cachetUrl").toString();
        String mainLabel = json.get("label").toString();
        urls.put(mainUrl, mainUrl);
        labels.put(mainLabel, mainLabel);

        Object obj = json.get("sources");
        if (obj == null) getSources().clear();
        else if (obj instanceof JSONObject){
            String url = ((JSONObject) obj).get("cachetUrl").toString();
            String label = ((JSONObject) obj).get("label").toString();
            if (mainUrl.equals(url)) {
                throw new Failure("Attempt adding duplicate Cachet urls - " + url);
            }
            if (mainLabel.equals(label) && !StringUtils.isEmpty(label)) {
                throw new Failure("Attempt adding duplicate labels - " + label);
            }
        }
        else if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            for (Object obj2 : arr) {
                JSONObject providerObj = (JSONObject) obj2;
                String url = providerObj.getString("cachetUrl");
                String label = providerObj.getString("label");
                if (urls.containsKey(url)) {
                    throw new Failure("Attempt adding duplicate Cachet urls - " + url);
                }
                if (labels.containsKey(label) && !StringUtils.isEmpty(label)) {
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

    public FormValidation doCheckCachetUrl(@QueryParameter String cachetUrl){
        if(StringUtils.isEmpty(cachetUrl)){
            return FormValidation.warning("Please provide a Cachet Url");
        }
        return FormValidation.ok();
    }
}
