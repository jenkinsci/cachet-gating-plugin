package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

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
public class CachetQueueTaskDispatcher extends QueueTaskDispatcher {

    private static final Logger log = Logger.getLogger(CachetQueueTaskDispatcher.class.getName());

    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        if (item.task instanceof Job) {
            Job<?,?> job = (Job<?,?>) item.task;
            CachetJobProperty property = job.getProperty(CachetJobProperty.class);

            CachetGatingAction gatingAction = item.getAction(CachetGatingAction.class);
            if (gatingAction == null) {
                gatingAction = new CachetGatingAction();
            }
            Map<String, CachetGatingMetrics> metricsMap = gatingAction.getGatingMetricsMap();
            if (property != null && property.getRequiredResources()) {
                List<String> requiredResourcesList = property.getResources();
                Map<String, Resource> watchedResources = ResourceProvider.SINGLETON.getResources(requiredResourcesList);
                if (watchedResources != null) {
                    List<String> msgs = new ArrayList<>();
                    for (String name : requiredResourcesList) {
                        CachetGatingMetrics requiredResourcesMetric = metricsMap.get(name);
                        if (requiredResourcesMetric == null) {
                            requiredResourcesMetric = new CachetGatingMetrics(name);
                            requiredResourcesMetric.setGateStartTime(System.currentTimeMillis());
                        }
                        if (!watchedResources.containsKey(name)) {
                            msgs.add(String.format("%s: %s", name, "Unknown resource"));
                            requiredResourcesMetric.setGateUpdatedTime(System.currentTimeMillis());
                            requiredResourcesMetric.setGatingStatus(ResourceStatus.UNKNOWN);
                        } else {
                            Resource r = watchedResources.get(name);
                            if (r.getStatusId() != ResourceStatus.OPERATIONAL) {
                                String message = String.format("%s: %s", name, r.getStatusName());
                                msgs.add(message);
                                log.info(message);
                                requiredResourcesMetric.setGateUpdatedTime(System.currentTimeMillis());
                                requiredResourcesMetric.setGatingStatus(r.getStatusId());
                            }
                        }
                        metricsMap.put(name, requiredResourcesMetric);
                    }
                    gatingAction.setGatingMetricsMap(metricsMap);
                    item.addOrReplaceAction(gatingAction);
                    if (msgs.size() > 0 && !property.getSkipBlockage()) {
                        return CauseOfBlockage.fromMessage(Messages._blockedMessage(StringUtils.join(msgs, "; ")));
                    }
                }
            }
        }
        return super.canRun(item);
    }
}
