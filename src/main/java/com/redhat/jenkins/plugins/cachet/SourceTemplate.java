package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;

public class SourceTemplate extends AbstractDescribableImpl<SourceTemplate> implements Serializable {

    private static final long serialVersionUID = 4112681620416495146L;

    private String cachetUrl;
    private String label;
    private boolean ignoreSSL;

    @DataBoundConstructor
    public SourceTemplate(String cachetUrl, String label, boolean ignoreSSL) {
        this.cachetUrl = cachetUrl;
        this.label = label;
        this.ignoreSSL = ignoreSSL;
    }

    public void setCachetUrl(String cachetUrl) {
        this.cachetUrl = cachetUrl;
    }

    String getCachetUrl() {
        return cachetUrl;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setIgnoreSSL(boolean ignoreSSL) {
        this.ignoreSSL = ignoreSSL;
    }

    boolean isIgnoreSSL() {
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
