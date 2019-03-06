package com.redhat.jenkins.plugins.cachet.pipeline;

import com.google.common.collect.ImmutableSet;
import com.redhat.jenkins.plugins.cachet.*;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.concurrent.TimeUnit;
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
    private final int timeLimit;
    private final boolean abortWhenTimeExceeded;
    private final List<String> resources;

    @DataBoundConstructor
    public CachetGatingWaitForResourcesStep(boolean abortWhenTimeExceeded, int timeLimit,List<String> resources) {
        this.resources = new ArrayList<>(resources);
        this.timeLimit = timeLimit;
        this.abortWhenTimeExceeded = abortWhenTimeExceeded;
    }

    public List<String> getResources(){
        return resources;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public boolean isAbortWhenTimeExceeded() {
        return abortWhenTimeExceeded;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(abortWhenTimeExceeded, timeLimit, resources, context);
    }

    public static class Execution extends SynchronousStepExecution<Map<String, Long>> {
        /**
         * Time limit in seconds
         */
        private transient final int timeLimit;
        private transient final boolean abortWhenTimeExceeded;
        private transient final List<String> resources;

        private static final float RECURRENCE_PERIOD_BACKOFF = 1.2f;
        static final long MIN_RECURRENCE_PERIOD = 15000; // ¼min
        static final long MAX_RECURRENCE_PERIOD = 45000;
        static long TIME_SO_FAR = 0;
        long recurrencePeriod = MIN_RECURRENCE_PERIOD;

        Execution(boolean abortWhenTimeExceeded, int timeLimit, List<String> resources, StepContext context) {
            super(context);
            this.resources = resources;
            this.timeLimit = timeLimit;
            this.abortWhenTimeExceeded = abortWhenTimeExceeded;
        }

        @Override
        protected Map<String, Long> run() throws Exception {
            Map<String, Long> waitForResourcesMet = new HashMap<>();
            List<String> resourcesToWaitFor = new ArrayList<>();

            Map<String, CachetGatingMetrics> metrics = getActionMetrics();
            // First iteration over metrics to determine if there is a resource we should wait for
            for (String resource : resources){
                if(!metrics.containsKey(resource))
                    throw new AbortException("One or more resources you want to wait for does not exist.");

                CachetGatingMetrics tmpResourceMet = metrics.get(resource);
                if(tmpResourceMet.getGatingStatus() == ResourceStatus.OPERATIONAL){
                    waitForResourcesMet.put(resource, tmpResourceMet.getGatedTimeElapsed());
                }
                else {
                    resourcesToWaitFor.add(resource);
                }
            }
            long timeInMillis = TimeUnit.SECONDS.toMillis(timeLimit);
            while (!resourcesToWaitFor.isEmpty()){// So there are resources we need to wait for
                sleepingHandler(resourcesToWaitFor);

                for (Iterator<String> iterator = resourcesToWaitFor.iterator(); iterator.hasNext();) {
                    String resource = iterator.next();
                    Resource tmpResource = getCurrentMetrics(resourcesToWaitFor).get(resource);
                    if(tmpResource != null
                            && tmpResource.getStatusName().equals("Operational")){
                        iterator.remove();
                        CachetGatingMetrics tmpResourceMet =  metrics.get(resource);
                        tmpResourceMet.setGateUpdatedTime(System.currentTimeMillis());

                        waitForResourcesMet.put(resource, tmpResourceMet.getGatedTimeElapsed());
                    }
                }
                if(timeInMillis <= TIME_SO_FAR && timeLimit != 0){
                    if(abortWhenTimeExceeded) throw new AbortException("Time limit exceeded - aborting ..");
                    else break;
                }
            }
            return waitForResourcesMet;
        }

        private Map<String, CachetGatingMetrics> getActionMetrics() throws Exception {
            Run run = getContext().get(Run.class);
            Action action = run.getAction(CachetGatingAction.class);
            if (action == null) {
                action = new CachetGatingAction();
            }
            return ((CachetGatingAction) action).getGatingMetricsMap();
        }

        private Map <String, Resource> getCurrentMetrics(List<String> resources) {
            return ResourceProvider.SINGLETON.getResources(resources);
        }

        private void sleepingHandler(List<String> resourcesToWaitFor) throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Waiting for " + resourcesToWaitFor.toString() + " for " + recurrencePeriod + " ms");
            TIME_SO_FAR += recurrencePeriod;
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
            return "cachetWaitForResources";
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
