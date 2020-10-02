# Cachet Gate Plugin

The Cachet Gating Plugin allows jobs to be held in the Jenkins queue based on Cachet resource availability.

## Global Configuration

Before the plugin may be used, you must configure the Cachet API URL in
the Jenkins global configuration:


### Configuration as Code


```yaml
unclassified:
  globalCachetConfiguration:
    cachetUrl: https://example.com
    ignoreSSL: false
```  

## Gating Jobs

To gate jobs, in the job configuration check the box to confirm resource
availability before building and then select the required resource from
the list. Note that **all** selected resources must be available for the
job to run.

Below is an example of a build that is blocked or gated:

![blocked job](https://raw.githubusercontent.com/jenkinsci/cachet-gating-plugin/master/docs/queue-blocked.png)

## Job setup using Job DSL

You can also use the job dsl plugin to configure gating for your jobs.
Here is an example snippet:

```groovy
properties {
  cachetJobProperty {
    requiredResources(true)
    resources(["service1", "service2"])
  }
}
```

## Gating metrics

You can use the following Jenkins pipeline snippet to gather metrics once build has
started after being gated:

```groovy
def metricsMap = cachetgatingmetrics()
if (metricsMap.size() > 0) {
  echo "Semaphore Required Resources Gating Metrics:"
} else {
  echo "This build was not gated by required resources not being available"
}


metricsMap.each{ k, v ->
       echo "- Resource name: ${k}"
       echo "- Status:        ${v.getGatingStatus()}"
       echo "- Elapsed time:  ${v.getGatedTimeElapsed()} ms"
       echo ""
}
```

## Job setup using JJB

From jenkins-job-builder 2.10.2 you can setup your jobs using JJB. Here
is an example snippet:

```yaml
properties:
      - cachet-gating:
          required-resources: true
          resources:
              - beaker
              - brew
```
