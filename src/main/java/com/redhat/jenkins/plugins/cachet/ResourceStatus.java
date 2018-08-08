package com.redhat.jenkins.plugins.cachet;

public enum ResourceStatus {
    UNKNOWN(0),
    OPERATIONAL(1),
    PERFORMANCE_ISSUES(2),
    PARTIAL_OUTAGE(3),
    MAJOR_OUTAGE(4);

    private final int id;

    ResourceStatus(int id) {
        this.id = id;
    }
    public int getValue() {
        return id;
    }
    public static ResourceStatus valueOf(Integer id) {
        for (ResourceStatus s : ResourceStatus.values()) {
            if (s.getValue() == id) {
                return s;
            }
        }
        return null;
    }
}
