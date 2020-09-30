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
package com.redhat.jenkins.plugins.cachet.pipeline;

import com.google.common.collect.ImmutableSet;
import com.redhat.jenkins.plugins.cachet.CachetGatingAction;
import com.redhat.jenkins.plugins.cachet.CachetGatingMetrics;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CachetGatingMetricsStep extends Step implements Serializable {

    @DataBoundConstructor
    public CachetGatingMetricsStep() {
    }

    @Override
    public StepExecution start(StepContext context) {
        return new CachetGatingMetricsStepExecution(this, context);
    }

    public static class CachetGatingMetricsStepExecution extends StepExecution {
        private static final long serialVersionUID = 6856767713701881593L;

        public CachetGatingMetricsStepExecution(CachetGatingMetricsStep s, StepContext context) {
            super(context);
        }

        @Override
        public boolean start() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            CachetGatingAction action = Objects.requireNonNull(run).getAction(com.redhat.jenkins.plugins.cachet.CachetGatingAction.class);
            if (action == null) {
                action = new CachetGatingAction();
            }
            Map<String, CachetGatingMetrics> metrics = action.getGatingMetricsMap();
            getContext().onSuccess(metrics);
            return true;
        }
    }
    /**
     * Adds the step as a workflow extension.
     */
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, Launcher.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "cachetgatingmetrics";
        }

        @Override
        public @Nonnull  String getDisplayName() {
            return "Cachet Gating Metrics";
        }
    }

    private static final long serialVersionUID = 1L;
}
