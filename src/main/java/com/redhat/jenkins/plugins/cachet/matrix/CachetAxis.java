package com.redhat.jenkins.plugins.cachet.matrix;

import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CachetAxis strings = (CachetAxis) o;
        return Objects.equals(resources, strings.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resources);
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public @Nonnull String getDisplayName() {
            return "Cachet Axis";
        }
    }
}
