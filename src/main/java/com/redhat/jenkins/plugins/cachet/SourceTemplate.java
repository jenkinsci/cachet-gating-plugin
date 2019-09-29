package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;

public class SourceTemplate extends AbstractDescribableImpl<SourceTemplate> implements Serializable {

    private static final long serialVersionUID = 2711426306506317834L;

    private final String cachetUrl;
    private final String label;
    private final boolean ignoreSSL;

    @DataBoundConstructor
    public SourceTemplate(String cachetUrl, String label, boolean ignoreSSL) {
        this.cachetUrl = cachetUrl;
        this.label = label;
        this.ignoreSSL = ignoreSSL;
    }

    public String getCachetUrl() {
        return cachetUrl;
    }

    public String getLabel() {
        return label;
    }

    public boolean isIgnoreSSL() {
        return ignoreSSL;
    }

    @Extension
    @Symbol("sourceTemplate")
    public static class DescriptorImpl extends Descriptor<SourceTemplate> {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Cachet Source";
        }
    }
}
