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
package com.redhat.jenkins.plugins.cachet;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;

public class CachetGatingMetrics implements Serializable {

    private String resourceName;
    private Long gateUpdatedTime;
    private ResourceStatus gatingStatus;
    private Long gateStartTime;

    @Override
    public String toString() {
        return "CachetGatingMetrics{" +
                "resourceName='" + resourceName + '\'' +
                ", gateUpdatedTime=" + gateUpdatedTime +
                ", gatingStatus='" + gatingStatus + '\'' +
                ", gateStartTime=" + gateStartTime +
                '}';
    }

    public CachetGatingMetrics(String resourceName, Long gateStartTime, Long gateUpdatedTime, ResourceStatus status) {
        this.resourceName = resourceName;
        this.gateStartTime = gateStartTime;
        this.gateUpdatedTime = gateUpdatedTime;
        this.gatingStatus = status;
    }

    @Whitelisted
    public Long getGateStartTime() {
        return gateStartTime;
    }

    public void setGateStartTime(Long gateStartTime) {
        if (this.gateStartTime == null) {
            this.gateStartTime = gateStartTime;
        }
    }

    public CachetGatingMetrics(String resourceName) {
        this.resourceName = resourceName;
        this.gatingStatus = ResourceStatus.OPERATIONAL;
    }

    @Whitelisted
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Whitelisted
    public Long getGateUpdatedTime() {
        return gateUpdatedTime;
    }

    @Whitelisted
    public Long getGatedTimeElapsed() {
        if (gateUpdatedTime == null || gateStartTime == null) {
            return 0L;
        }
        return gateUpdatedTime - gateStartTime;
    }

    public void setGateUpdatedTime(Long gateUpdatedTime) {
        this.gateUpdatedTime = gateUpdatedTime;
    }

    public void setGatingStatus(ResourceStatus status) {
        this.gatingStatus = status;
    }

    @Whitelisted
    public ResourceStatus getGatingStatus() {
        return gatingStatus;
    }

    private static final long serialVersionUID = 1L;
}
