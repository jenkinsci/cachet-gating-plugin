package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

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
public class CachetJobProperty extends JobProperty<Job<?, ?>> {
    private Boolean skipBlockage;
    private Boolean requiredResources;
    private List<String> resources;

    @DataBoundConstructor
    public CachetJobProperty(Boolean skipBlockage, Boolean requiredResources, List<String> resources) {
        this.skipBlockage = skipBlockage;
        this.requiredResources = requiredResources;
        this.resources = resources;
    }

    public Boolean getSkipBlockage() {
        return skipBlockage;
    }

    public void setSkipBlockage(Boolean skipBlockage) {
        this.skipBlockage = skipBlockage;
    }

    public Boolean getRequiredResources() {
        return requiredResources;
    }

    public void setRequiredResources(Boolean requiredResources) {
        this.requiredResources = requiredResources;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    @Extension
    public static class CachetJobPropertyDescriptor extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.pluginName();
        }

        public List<String> getResourceNames() {
            return ResourceProvider.SINGLETON.getResourceNames();

        }
    }
}
