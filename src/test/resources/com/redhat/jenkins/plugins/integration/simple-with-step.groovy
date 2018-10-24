node() {
    def metricsMap = cachetgatingmetrics()

    metricsMap.each{ k, v ->
        echo "resource name: ${k}"
        echo "status:        ${v.getGatingStatus()}"
        echo "elapsed time:  ${v.getGatedTimeElapsed()}"
    }

}
