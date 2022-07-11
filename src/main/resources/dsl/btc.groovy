package dsl
// vars/btc.groovy

String getEpContainerIP(container) {
    String containerId = null
    try {
        containerId = container.id
        String ipAddr = sh returnStdout: true, script: "#!/bin/sh -e\n" + "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' " + container.id
        ipAddr = ipAddr.trim()
        return ipAddr
    } catch (Exception e) {
        error("Unable to retrieve IP address of container " + (containerId != null ? containerId : ""))
    }
    return null

}

