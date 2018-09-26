package com.redhat.jenkins.plugins.cachet;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

@Extension
public class CachetQueueTaskDispatcher extends QueueTaskDispatcher {

    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        if (item.task instanceof Job) {
            Job<?,?> job = (Job<?,?>) item.task;
            CachetJobProperty property = job.getProperty(CachetJobProperty.class);

            if (property != null && property.getRequiredResources()) {
                Map<String, Resource> resources = ResourceProvider.SINGLETON.getResources(property.getResources());
                if (resources != null) {
                    List<String> msgs = new ArrayList<>();
                    for (String name : property.getResources()) {
                        if (!resources.containsKey(name)) {
                            msgs.add(String.format("%s: %s", name, "Unknown resource"));
                        } else {
                            Resource r = resources.get(name);
                            if (r.getStatusId() != ResourceStatus.OPERATIONAL) {
                                msgs.add(String.format("%s: %s", name, r.getStatusName()));
                            }
                        }
                    }
                    if (msgs.size() > 0) {
                        return CauseOfBlockage.fromMessage(Messages._blockedMessage(StringUtils.join(msgs, "; ")));
                    }
                }
            }
        }

        return super.canRun(item);
    }
}
