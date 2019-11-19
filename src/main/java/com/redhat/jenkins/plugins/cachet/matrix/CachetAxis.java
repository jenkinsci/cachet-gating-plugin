package com.redhat.jenkins.plugins.cachet.matrix;

import com.redhat.jenkins.plugins.cachet.ResourceProvider;
import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.List;

public class CachetAxis extends Axis {
    private List<String> resources;

    @DataBoundConstructor
    public CachetAxis(String name, List<String> resources) {
        super(name, resources);
        this.resources = resources;
    }

    public List<String> getResources() {
        return resources != null ? resources : Collections.emptyList();
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public String getDisplayName() {
            return "Cachet Axis";
        }

        public List<String> getResourceNames() {
            return ResourceProvider.SINGLETON.getResourceNames();
        }
    }
}
