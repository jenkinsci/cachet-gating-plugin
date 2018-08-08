package com.redhat.jenkins.plugins.cachet;


public class Resource {

    private String name;
    private ResourceStatus statusId;
    private String statusName;

    public Resource(String name) {
        this.name = name;
    }

    public Resource(String name, ResourceStatus statusId) {
        this.name = name;
        this.statusId = statusId;
    }

    public Resource(String name, ResourceStatus statusId, String statusName) {
        this(name, statusId);
        this.statusName = statusName;
    }

    public Resource(String name, String statusName) {
        this(name);
        this.statusName = statusName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceStatus getStatusId() {
        return statusId;
    }

    public void setStatusId(ResourceStatus statusId) {
        this.statusId = statusId;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

}
