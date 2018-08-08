package com.redhat.jenkins.plugins.cachet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

public enum ResourceProvider {
    SINGLETON;

    private static final Logger log = Logger.getLogger(ResourceProvider.class.getName());

    private static final String ATTRIBUTE_STATUS = "status";
    private static final String ATTRIBUTE_STATUS_NAME = "status_name";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, JsonNode> resources;

    public void setResources(Map<String, JsonNode> resources) {
        lock.writeLock().lock();;
        try {
            this.resources = resources;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unhandled exception setting resources.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Resource> getResources(List<String> names) {
        if (resources != null) {
            lock.readLock().lock();
            try {
                Map<String, Resource> results = new HashMap<>();
                for (String name : names) {
                    try {
                        Resource x = new Resource(name);
                        x.setStatusId(ResourceStatus.UNKNOWN);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Exception!", e);
                    }
                    Resource r = new Resource(name, ResourceStatus.UNKNOWN, "Unknown");
                    if (resources.containsKey(name)) {
                        JsonNode node = resources.get(name);
                        if (node.has(ATTRIBUTE_STATUS)) {
                            r.setStatusId(ResourceStatus.valueOf(node.get(ATTRIBUTE_STATUS).asInt()));
                            if (node.has(ATTRIBUTE_STATUS_NAME)) {
                                r.setStatusName(node.get(ATTRIBUTE_STATUS_NAME).asText());
                            }
                        }
                    }
                    results.put(name, r);
                }
                return results;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unhandled exception getting resources.", e);
            } finally {
                lock.readLock().unlock();
            }
        }
        return null;
    }

    public List<String> getResourceNames() {
        if (resources != null) {
            lock.readLock().lock();
            try {
                return new ArrayList<String>(resources.keySet());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unhandled exception getting resource names.", e);
            } finally {
                lock.readLock().unlock();
            }
        }
        return null;
    }
}
