package com.redhat.jenkins.plugins.cachet.pipeline;

import com.google.common.collect.ImmutableSet;
import com.redhat.jenkins.plugins.cachet.CachetGatingAction;
import com.redhat.jenkins.plugins.cachet.CachetGatingMetrics;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
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

public class CachetGatingWaitForResourcesStep extends Step implements Serializable {

    private final List<String> resources;

    @DataBoundConstructor
    public CachetGatingWaitForResourcesStep(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public List<String> getResources(){
        return resources;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(resources, context);
    }

    public static class Execution extends SynchronousStepExecution<Map<String, Long>> {

        private transient final List<String> resources;

        private static final float RECURRENCE_PERIOD_BACKOFF = 1.2f;
        static final long MIN_RECURRENCE_PERIOD = 250; // ¼s
        static final long MAX_RECURRENCE_PERIOD = 15000; // ¼min
        long recurrencePeriod = MIN_RECURRENCE_PERIOD;

        Execution(List<String> resources, StepContext context) {
            super(context);
            this.resources = resources;
        }

        @Override
        protected Map<String, Long> run() throws Exception {
            Map<String, Long> waitForResourcesMet = new HashMap<>();
            List<String> resourcesToWaitFor = new ArrayList<>();

            Map<String, CachetGatingMetrics> metrics = getCurrentMetrics();

            for (String resource : resources){
                if(!metrics.containsKey(resource))
                    throw new AbortException("One or more resources you want to wait for does not exist.");

                CachetGatingMetrics tmpResourceMet = metrics.get(resource);
                if(tmpResourceMet.getGatingStatus().equals("Operational")){
                    waitForResourcesMet.put(resource, tmpResourceMet.getGatedTimeElapsed());
                }
                else {
                    resourcesToWaitFor.add(resource);
                }
            }
            
            while (!resourcesToWaitFor.isEmpty()){// So there are resources we need to wait for
                sleepingHandler(resourcesToWaitFor);
                for (String resource : resourcesToWaitFor){
                    CachetGatingMetrics tmpResourceMet = getCurrentMetrics().get(resource);
                    if(tmpResourceMet.getGatingStatus().equals("Operational")){
                        resourcesToWaitFor.remove(resource);
                        waitForResourcesMet.put(resource, tmpResourceMet.getGatedTimeElapsed());
                    }
                }
            }
          return waitForResourcesMet;
        }

        /**
         * Help method
         * @return The current metrics
         * @throws Exception
         */
        public Map<String, CachetGatingMetrics> getCurrentMetrics() throws Exception {
            Run run = getContext().get(Run.class);
            Action action = run.getAction(com.redhat.jenkins.plugins.cachet.CachetGatingAction.class);
            if (action == null) {
                action = new CachetGatingAction();
            }
            return ((CachetGatingAction) action).getGatingMetricsMap();
        }

        /**
         * Handle the waiting for the resources
         * @param listener
         * @param resourcesToWaitFor
         * @throws Exception
         */
        public void sleepingHandler(List<String> resourcesToWaitFor) throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Waiting for " + resourcesToWaitFor.toString() + " for " + recurrencePeriod + " ms");
            Thread.sleep(recurrencePeriod);
            recurrencePeriod = Math.min((long)(recurrencePeriod * RECURRENCE_PERIOD_BACKOFF), MAX_RECURRENCE_PERIOD);
        }
    }

    /**
     * Adds the step as a workflow extension.
     */
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "cachet-wait-for-resources";
        }

        @Override
        public String getDisplayName() {
            return "Cachet Gating Check Resources Status";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, Launcher.class, TaskListener.class);
        }
    }
    private static final long serialVersionUID = 1L;
}
