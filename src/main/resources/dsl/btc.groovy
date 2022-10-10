package dsl
// vars/btc.groovy

def epJenkinsPort = null       // the port to use for communication with EP
def isDebug  = false       // specifies if a debug environment should be exported and archived (true, false)
def mode     = null       // mode for migration suite (source, target)

/**
 * Connects to a running instance of BTC EmbeddedPlatform.
 * If EP is not available, availability is checked until success
 * or until the timeout (default: 2 minutes) has expired.
 */
def connect(body = {}) {
    // important check, prevents trouble down the line
    if (env.USERPROFILE.toLowerCase().contains('system32')) {
        error('Unsupported "Local System" account detected. Jenkins Agent must be configured to run processes as a dedicated user.')
    }

    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    if (config.port != null) {
        epJenkinsPort = config.port
    } else {
        epJenkinsPort = "29267" // default as fallback
    }
    def timeoutSeconds = 120
    if (config.timeout != null)
        timeoutSeconds = config.timeout
    def responseCode = 500;
    if (isEpAvailable()) {
        printToConsole("(200) Successfully connected to a running instance of BTC EmbeddedPlatform on port: ${epJenkinsPort}.")
        responseCode = 200 // connected to an existing instance
    } else {
        timeout(time: timeoutSeconds, unit: 'SECONDS') { // timeout for connection to EP
            try {
                waitUntil(quiet: true, initialRecurrencePeriod: 3000) {
                    // exit waitUntil closure
                    return isEpAvailable()
                }
                printToConsole("(200) Successfully connected to a running instance of BTC EmbeddedPlatform on port: ${epJenkinsPort}.")
                responseCode = 200 // connected to an existing instance
            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException err) {
                error("(400) Connection attempt to BTC EmbeddedPlatform timed out after " + timeoutSeconds + " seconds.")
            }
        }
    }
    return responseCode
}

/**
 * Start BTC EmbeddedPlatform on the node.
 * If EP is not available a new instance is started via a powershell command. Availability
 * is then checked until success or until the timeout (default: 2 minutes) has expired.
 */
def startup(body = {}) {
    // important check, prevents trouble down the line
    if (env.USERPROFILE.toLowerCase().contains('system32')) {
        error('Unsupported "Local System" account detected. Jenkins Agent must be configured to run processes as a dedicated user.')
    }

    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    epJenkinsPort = config.port != null ? config.port : "29267"
    def timeoutSeconds = config.timeout != null ? config.timeout : 120
    
    /*
    * EP Startup configuration
    */
    def epInstallDir = null
    if (config.installPath != null) {
        epInstallDir = "${config.installPath}".replace("/", "\\")
    } else {
        try {
            def epRegPath = bat returnStdout: true, label: 'Query Registry for active EP version', script: '''
            @echo OFF
            for /f "tokens=*" %%a in ('REG QUERY HKLM\\SOFTWARE\\BTC /reg:64 /s /f "EmbeddedPlatform" ') do (
                call :parseEpVersion %%a
            )
            :parseEpVersion
                set key=%1 %2
                if "%key:~0,4%" == "HKEY" (
                    for /f "tokens=2*" %%a in ('REG QUERY "%key%" /reg:64 /v EPACTIVE') do (
                        if %%b == 1 (
                            for /f "tokens=2*" %%a in ('REG QUERY "%key%" /reg:64 /v Path') do ( echo %%b )
                        )
                    )
                )
            '''
            def split = "${epRegPath}".trim().split("\n")
            epInstallDir = split[split.length - 1].trim()
        } catch (err) {
            error("The active version of BTC EmbeddedPlatform could not be queried from your registry. You can pass the installation path to the startup method (installPath = ...) to work around this issue. Please note that this version of BTC EmbeddedPlatform still needs to be installed and integrated correctly in order to work properly.")
        }
    }
    epVersion = null
    try {
        def split = "${epInstallDir}".split("\\\\")
        epVersion = split[split.length - 1].substring(2)
    } catch (err) {
        error("Invalid path to BTC EmbeddedPlatform installation: ${epInstallDir}")
    }
    
    def responseCode = 500
    // configure license packages (required since 2.4.0 to support ET_BASE)
    def licensingPackage = config.licensingPackage
    if (licensingPackage == null) {
        licensingPackage = 'ET_COMPLETE,ET_AUTOMATION_SERVER'
    } else if (licensingPackage.equals('ET_COMPLETE')) {
        licensingPackage += ',ET_AUTOMATION_SERVER'
    } else if (licensingPackage.equals('ET_BASE')) {
        licensingPackage += ',ET_AUTOMATION_SERVER_BASE'
    }
    epJenkinsPort = findNextAvailablePort(epJenkinsPort)

    def preferenceDir = config.preferenceDir != null ? config.preferenceDir : "${env:programdata}/BTC/ep/${epVersion}/EPPreferences"
    if (!fileExists(preferenceDir)) {
        def ignored = bat returnStdout: true, script: "@echo off & mkdir \"$preferenceDir\" 1>nul 2>nul"
    }
    printToConsole("Connecting to EP ${epVersion} using port ${epJenkinsPort}. (timeout: " + timeoutSeconds + " seconds).\n(The log file on the agent can be found here: ${env:userprofile}/AppData/Roaming/BTC/ep/${epVersion}/${epJenkinsPort}/logs/current.log)")
    timeout(time: timeoutSeconds, unit: 'SECONDS') { // timeout for connection to EP
        try {
            epJreDir = getJreDir(epInstallDir)
            epJreString = ''
            if (epJreDir != null) {
                epJreString = " '-vm', '\"${epJreDir}\"',"
            }
            def startCmd = """\$p = Start-Process '${epInstallDir}/rcp/ep.exe' -PassThru -ArgumentList \
                '-clearPersistedState', \
                '-application', """
                 // before/after package refactoring
            if (compareVersions(epVersion, '2.11p0') >= 0) {
                startCmd += "'ep.application.headless', "
            } else { // version < 2.11
                startCmd += "'com.btc.ep.application.headless', "
            }
            startCmd += """'-nosplash',${epJreString} \
                '-vmargs', \
                \"-Dosgi.configuration.area.default=`\"\${env:userprofile}/AppData/Roaming/BTC/ep/${epVersion}/${epJenkinsPort}/configuration`\"\", \
                \"-Dosgi.instance.area.default=`\"\${env:userprofile}/AppData/Roaming/BTC/ep/${epVersion}/${epJenkinsPort}/workspace`\"\", \
                '-Dep.configuration.logpath=AppData/Roaming/BTC/ep/${epVersion}/${epJenkinsPort}/logs', \
                '-Dep.runtime.workdir=BTC/ep/${epVersion}/${epJenkinsPort}', \
                '-Dep.preference.dir=${preferenceDir}', \
                '-Dep.licensing.package=${licensingPackage}',"""
            // before/after package refactoring
            if (compareVersions(epVersion, '2.11p0') >= 0) {
                startCmd += "'-Dep.runtime.batch=ep',"
            } else { // version < 2.11
                startCmd += "'-Dep.runtime.batch=com.btc.ep',"
            }
            // use rest port / jenkins port
            if (compareVersions(epVersion, '2.8p0') >= 0) {
                startCmd += " '-Dep.jenkins.port=${epJenkinsPort}', '-Djna.nosys=true', '-Dprism.order=sw', '-XX:+UseParallelGC'"
            } else { // version < 2.8
                startCmd += " '-Dep.rest.port=${epJenkinsPort}'"
            }
            if (config.additionalJvmArgs != null) {
                startCmd += ", '${config.additionalJvmArgs}'"
            } else {
                startCmd += ", '-Xmx2g'"
            }
            startCmd += '; echo $p.id'
            def startCmdOutput = powershell label: 'Starting BTC EmbeddedPlatform', returnStdout: true, script: startCmd
            PID = startCmdOutput.trim()
            reservePortAndReleaseRegistryLock(epJenkinsPort)
            waitUntil(quiet: true, initialRecurrencePeriod: 3000) {
                r = httpRequest quiet: true, url: "http://localhost:${epJenkinsPort}/check", validResponseCodes: '100:500'
                // exit waitUntil closure
                return (r.status == 200)
            }
            printToConsole("(200) Successfully started and connected to BTC EmbeddedPlatform ${epVersion} on port: ${epJenkinsPort}.")
            responseCode = 200 // connected to a new instance
        } catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException err) {
            error("(400) Connection attempt to BTC EmbeddedPlatform timed out after " + timeoutSeconds + " seconds.")
        }
    }
    return responseCode
}

def killEp(body = {}) {
    // if we don't have a PID (e.g., because the process was started earlier and we connected to it)
    // we use the port to find the PID
    if ((!binding.hasVariable('PID') || PID == null) && (binding.hasVariable('epJenkinsPort') && epJenkinsPort != null)) {
        def nsOut = powershell returnStdout: true, script: "netstat -ona -p tcp | Select-String 0.0.0.0:${epJenkinsPort} | Select-String LISTENING"
        PID = nsOut.trim().split(/\s/).last()
    }
    def status = bat returnStatus: true, script: "@echo off & taskkill /f /pid $PID 1>nul 2>nul"
    if (status == 0) {
        printToConsole("Successfully closed the BTC EmbeddedPlatform instance.")
    }
}

/**
 * versions must match the format <number>.<number>p<number>
 * e.g. 2.10p3
 */
def compareVersions(v1, v2) {
    def regex = /(\d+)\.(\d+)p(\d+)/
    int v1_1 = Integer.parseInt(v1.replaceAll(regex, '$1'))
    int v2_1 = Integer.parseInt(v2.replaceAll(regex, '$1'))
    int comparison = v1_1.compareTo(v2_1)
    if (comparison != 0) {
        return comparison
    }

    int v1_2 = Integer.parseInt(v1.replaceAll(regex, '$2'))
    int v2_2 = Integer.parseInt(v2.replaceAll(regex, '$2'))
    comparison = v1_2.compareTo(v2_2)
    if (comparison != 0) {
        return comparison
    }

    int v1_3 = Integer.parseInt(v1.replaceAll(regex, '$3'))
    int v2_3 = Integer.parseInt(v2.replaceAll(regex, '$3'))
    comparison = v1_3.compareTo(v2_3)
    return comparison
}

def handleError(e) {
    try { wrapUp {} } catch (err) {} finally { printToConsole(e) }
}

// Profile Creation steps

def profileLoad(body) {
    return profileInit(body, 'profileLoad')
}

def profileCreateTL(body) {
    return profileInit(body, 'profileCreateTL')
}

def profileCreateEC(body) {
    def config = resolveConfig(body)
    config.enableEC = true
    return profileInit(config, 'profileCreateEC')
}

def profileCreateSL(body) {
    return profileInit(body, 'profileCreateSL')
}

def profileCreateC(body) {
    return profileInit(body, 'profileCreateC')
}

def profileInit(body, method) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, method)
    // call EP to invoke profile creation / loading / update
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/${method}", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    if (r.status >= 400) {
        def relativeReportPath = toRelPath(exportPath)
        publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: "${relativeReportPath}", reportFiles: 'ProfileMessages.html', reportName: 'Profile Messages'])
        error("Error during profile load / creation.")
    }
    return r.status
}

// Normal Steps

def testExecutionReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'testExecutionReport')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/testExecutionReport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def codeAnalysisReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'codeAnalysisReport')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/codeAnalysisReport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def modelCoverageReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'modelCoverageReport')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/modelCoverageReport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def interfaceReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'interfaceReport')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/interfaceReport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def xmlReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'xmlReport')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/xmlReportExport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def rbtExecution(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'rbtExecution')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/testexecution", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def formalTest(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'formalTest')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/formalTest", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def vectorGeneration(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'vectorGeneration')
    // call EP to invoke vector generation and analysis
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/vectorGeneration", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def backToBack(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'backToBack')
    // call EP to invoke back-to-back test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/backToBack", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def regressionTest(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'regressionTest')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/regressionTest", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def rangeViolationGoals(body) {
    if (compareVersions(epVersion, '2.9p0') >= 0) {
        // this method only exist in older versions
        printToConsole(" -> Skipped RVG step (has been replaced by btc.addDomainCheckGoals)")
        return 400
    } else {
        // evaluate the body block, and collect configuration into the object
        def config = resolveConfig(body)
        def reqString = createReqString(config, 'rangeViolationGoals')
        // call EP to invoke test execution
        def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addRangeViolationGoals", validResponseCodes: '100:500'
        printToConsole(" -> (${r.status}) ${r.content}")
        return r.status
    }
}

def addInputCombinationGoals(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'inputCombinationGoals')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addUdcgInputCombinationGoals", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def domainCoverageGoals(body) {
    if (!binding.hasVariable('epVersion') || compareVersions(epVersion, '2.9p0') >= 0) {
        // this method only exist in older versions
        printToConsole(" -> Skipped DCG step (has been replaced by btc.addDomainCheckGoals)")
        return 400
    } else {
        // evaluate the body block, and collect configuration into the object
        def config = resolveConfig(body)
        def reqString = createReqString(config, 'domainCoverageGoals')
        // call EP to invoke test execution
        def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addDomainCoverageGoals", validResponseCodes: '100:500'
        printToConsole(" -> (${r.status}) ${r.content}")
        return r.status
    }
}

def addDomainCheckGoals(body) {
    if (binding.hasVariable('epVersion') && compareVersions(epVersion, '2.9p0') >= 0) {
        // evaluate the body block, and collect configuration into the object
        def config = resolveConfig(body)
        def reqString = createReqString(config, 'addDomainCheckGoals')
        // call EP to invoke test execution
        def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addDomainCheckGoals", validResponseCodes: '100:500'
        printToConsole(" -> (${r.status}) ${r.content}")
        return r.status
    } else {
        // this method only exist in older versions
        printToConsole(" -> Skipped Domain Checks Goals step (not available before EP 2.9p0)")
        return 400
    }
}

def vectorImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'vectorImport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/vectorImport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def vectorExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'vectorExport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/vectorExport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def toleranceImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'toleranceImport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/toleranceImport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def toleranceExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'toleranceExport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/toleranceExport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def inputRestrictionsImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'inputRestrictionsImport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/inputRestrictionsImport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def inputRestrictionsExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'inputRestrictionsExport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/inputRestrictionsExport", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def formalVerification(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'formalVerification')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/formalVerification", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def executionRecordExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'executionRecordExport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/exportExecutionRecords", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def executionRecordImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'executionRecordImport')
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/importExecutionRecords", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def removeIncompatibleVectors(body) {
    // call EP
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'removeIncompatibleVectors')
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/removeIncompatibleVectors", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

/*
 * special PLUGIN use case
 */
def setDefaultTolerances(body) {
    // call EP
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'setDefaultTolerances')
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/setDefaultTolerances", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def defaultTolerances(body) {
    // call EP
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'defaultTolerances')
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/defaultTolerances", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def collectProjectOverview(body)  {
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'collectProjectOverview')
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/collectProjectOverview", validResponseCodes: '100:500'
    projectOverview = readJSON text: r.content
    if (!binding.hasVariable('projectOverviewList')) {
        initProjectOverview()
    }
    projectOverviewList.add(projectOverview)
    printToConsole(" -> (${r.status}) Collected project status for overview report.")
    return r.status
}

def createOverallReport(path)  {
    def config = [:]
    config.path = toAbsPath(path)
    config.projects = projectOverviewList
    def payload = toJson(config)
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: payload, url: "http://localhost:${epJenkinsPort}/createOverallReport", validResponseCodes: '100:500'
    if (r.status == 404) {
        printToConsole(" -> Overview Report feature not supported by the JenkinsAutomation Plugin version you have installed on the Agent machine.")
    } else {
        printToConsole(" -> (${r.status}) ${r.content}")
    }
    return r.status
}

// wrap up step

/**
 * This method publishes html reports to the Jenkins Job page, archives artifacts
 * (profiles, debug zip files, etc.) and passes test results to JUnit.
 * In the end it closes EP. Which is important to release CPU / RAM resources.
 * - If Matlab has been opened by EP it will be closed automatically
 * - If Matlab was already available and EP just connected to it, Matlab will not be closed
 *
 * The boolean properties archiveProfiles, publishReports and publishResults allow users
 * to skip the respective steps by setting the property to false.
 */
def wrapUp(body = {}) {
    def config = resolveConfig(body)
    def archiveProfiles = true
    if (config.archiveProfiles != null)
        archiveProfiles = config.archiveProfiles
    def publishReports = true
    if (config.publishReports != null)
        publishReports = config.publishReports
    def publishResults = true
    if (config.publishResults != null)
        publishResults = config.publishResults
    try {
        // Closes BTC EmbeddedPlatform. Try-Catch is needed because the REST API call
        // will throw an exception as soon as the tool closes. This is expected.
        def reqString = "" // removed closeEp parameter, it was only causing problems
        if (config.closeEp != null && config.closeEp == false) {
            reqString = "false"
        }
        // this overrides "false" or anything else
        if (config.exit != null && config.exit == true) {
            reqString = "exit"
        }
        httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        printToConsole('BTC EmbeddedPlatform successfully closed.')
    } finally {
        if (config.closeEp == null || config.closeEp == true) {
            releasePort(epJenkinsPort)
            killEp()
        }
    }
    try {
        def relativeReportPath = null
        if ((binding.hasVariable('isDebug') && isDebug) || publishReports) {
            relativeReportPath = toRelPath(exportPath)
        }
        // archiveArtifacts works with relative paths
        if (archiveProfiles) {
            def profilePathParentDir = getParentDir(toRelPath(profilePath))
            archiveArtifacts allowEmptyArchive: true, artifacts: "${profilePathParentDir}/*.epp"
        }
        if (binding.hasVariable('isDebug') && isDebug)
            archiveArtifacts allowEmptyArchive: true, artifacts: "${relativeReportPath}/Debug_*.zip"
        // fileExists check needs absolute path
        if (publishReports && fileExists("${exportPath}/TestAutomationReport.html"))
            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: "${relativeReportPath}", reportFiles: 'TestAutomationReport.html', reportName: 'Test Automation Report'])
    } catch (err) {
        printToConsole(err.message)
    }
    // JUnit works with relative paths
    if (publishResults) {
        printToConsole("Looking for junit results in '" + "**/*junit-report.xml" + "'")
        junit allowEmptyResults: true, testResults: "**/*junit-report.xml"
    }
}

def finalWrapUp(body = {}) {
    def config = resolveConfig(body)
    def relativePath = toRelPath(config.path)
    createOverallReport("${relativePath}/OverviewReport.html")
    try {
        // Closes BTC EmbeddedPlatform. Try-Catch is needed because the REST API call
        // will throw an exception as soon as the tool closes. This is expected.
        httpRequest quiet: true, httpMode: 'POST', requestBody: 'exit', url: "http://localhost:${epJenkinsPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        printToConsole('BTC EmbeddedPlatform successfully closed.')
    } finally {
        releasePort(epJenkinsPort)
    }
    if (fileExists("${relativePath}/OverviewReport.html")) {
        publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: "${relativePath}", reportFiles: 'OverviewReport.html', reportName: 'Test Automation Overview Report'])
    }
    junit allowEmptyResults: true, testResults: "**/*junit-report.xml"
}

/**
 * Creates a profile on the source configuration (e.g. old Matlab / TargetLink version),
 * generates vectors for full coverage and exports the simulation results.
 */
def migrationSource(body) {
    // activate mode: target
    mode = 'source'
    
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def stashName = null
    // dispatch args
    if (config.uniqueName != null) {
        migrationTmpDir = toAbsPath("Migration/${config.uniqueName}")
        stashName = "migration-source-${config.uniqueName}"
    } else {
        migrationTmpDir = toAbsPath("MigrationTmp")
        stashName = "migration-source"
    }
    config.migrationTmpDir = toAbsPath(migrationTmpDir)
    config.exportPath = migrationTmpDir + "/reports"
    //config.saveProfileAfterEachStep = true
    
    // Startup
    if (config.uniqueName != null || config.isDocker) {
        connect(body)
    } else {
        startup(body)
    }
    // Profile Creation
    if (config.profilePath == null) {
        // create a new profile
        if (config.uniqueName != null) {
            config.profilePath = migrationTmpDir + "/profiles/${config.uniqueName}.epp"
        } else {
            config.profilePath = migrationTmpDir + "/profiles/profile.epp"
        }
        if (config.tlModelPath != null) {
            r = profileCreateTL(config)
        } else if (config.slModelPath != null) {
            r = profileCreateEC(config)
        } else if (config.codeModelPath != null) {
            r = profileCreateC(config)
        } else {
            error("Please specify the model to be tested (source configuration).")
        }
    } else {
        // load existing profile
        config.updateRequired = true
        r = profileLoad(config)
    }
    if (r >= 300) {
        wrapUp(body)
        error("Error during profile creation (source)")
    }
    
    // Import vectors if requested
    if (config.importDir != null) {
        vectorImport(body)
    }

    // Vector Generation
    if (config.createReport == null) {
        config.createReport = true
    }
    vectorGeneration(config)

    // Import vectors if requested
    if (config.exportDir != null) {
        vectorExport(body)
    }

    // Simulation
    r = regressionTest(config)
    if (r >= 400) {
        wrapUp(body)
        error("Error during simulation on source config.")
    }
    // ER Export
    executionRecordExport {
        dir = "${this.migrationTmpDir}/er/SIL"
        executionConfig = "SIL"
        isMigration = true
    }
    if ("${config.executionConfigString}".contains("TL MIL")) {
        executionRecordExport { // only if requested
            dir = "${this.migrationTmpDir}/er/TL_MIL"
            executionConfig = "TL MIL"
            isMigration = true
        }
    }
    if ("${config.executionConfigString}".contains("SL MIL")) {
        executionRecordExport { // only if requested
            dir = "${this.migrationTmpDir}/er/SL_MIL"
            executionConfig = "SL MIL"
            isMigration = true
        }
    }
    if ("${config.executionConfigString}".contains("PIL")) {
        executionRecordExport { // only if requested
            dir = "${this.migrationTmpDir}/er/PIL"
            executionConfig = "PIL"
            isMigration = true
        }
    }
    try {
        def reqString = "MIGRATION_SOURCE"
        if (config.uniqueName != null || config.isDocker) {
            reqString += ";false"
        }
        httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        printToConsole('BTC EmbeddedPlatform successfully closed.')
    } finally {
        releasePort(epJenkinsPort)
    }
    def relativeMigTmpDir = toRelPath(migrationTmpDir)
    def relativeExportPath = toRelPath(exportPath)
    // only archive profiles if source profiles are expected
    if (config.saveProfileAfterEachStep == true) {
        archiveArtifacts allowEmptyArchive: true, artifacts: "${relativeMigTmpDir}/profiles/*.epp"
    }
    // stash mdf files and reports
    printToConsole("stashing files: ${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/*")
    stash includes: "${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/**/*", name: stashName
}

/**
 * Creates a profile on the target configuration (e.g. newMatlab / TargetLink version),
 * imports the simulation results from the source config and runs a regression test.
 */
def migrationTarget(body) {
    // activate mode: target
    mode = 'target'
    
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    
    // dispatch args
    def stashName = null
    if (config.uniqueName != null) {
        migrationTmpDir = toAbsPath("Migration/${config.uniqueName}")
        stashName = "migration-source-${config.uniqueName}"
    } else {
        migrationTmpDir = toAbsPath("MigrationTmp")
        stashName = "migration-source"
    }
    config.migrationTmpDir = toAbsPath(migrationTmpDir)
    config.exportPath = migrationTmpDir + "/reports"
    //config.saveProfileAfterEachStep = true
    config.loadReportData = true
    
    // unstash files from source slave
    unstash stashName
    
    // Startup
    if (config.uniqueName != null || config.isDocker) {
        connect(body)
        config.closeEp = false
    } else {
        startup(body)
    }
    // Profile Creation
    if (config.profilePath == null) {
        // create a new profile
        if (config.uniqueName != null) {
            config.profilePath = migrationTmpDir + "/profiles/${config.uniqueName}.epp"
        } else {
            config.profilePath = migrationTmpDir + "/profiles/profile.epp"
        }
        if (config.tlModelPath != null) {
            r = profileCreateTL(config)
        } else if (config.slModelPath != null) {
            r = profileCreateEC(config)
        } else if (config.codeModelPath != null) {
            r = profileCreateC(config)
        } else {
            error("Please specify the model to be tested (target configuration).")
        }
    } else {
        // load existing profile
        config.updateRequired = true
        r = profileLoad(config)
    }
    if (r >= 300) {
        wrapUp(body)
        error("Error during profile creation (target)")
    }
    
    // ER Import
    executionRecordImport { // always test SIL vs. SIL
        dir = "${this.migrationTmpDir}/er/SIL"
        executionConfig = "SIL"
    }
    if ("${config.executionConfigString}".contains("TL MIL")) {
        executionRecordImport { // only if requested
            dir = "${this.migrationTmpDir}/er/TL_MIL"
            executionConfig = "TL MIL"
        }
    }
    if ("${config.executionConfigString}".contains("SL MIL")) {
        executionRecordImport { // only if requested
            dir = "${this.migrationTmpDir}/er/SL_MIL"
            executionConfig = "SL MIL"
        }
    }
    if ("${config.executionConfigString}".contains("PIL")) {
        executionRecordImport { // only if requested
            dir = "${this.migrationTmpDir}/er/PIL"
            executionConfig = "PIL"
        }
    }
    // Regression Test
    r = regressionTest(config)
    
    // contribute to overall report
    if (config.uniqueName != null) {
        collectProjectOverview{}
    }

    // Wrap Up
    if (config.uniqueName != null) {
        wrapUp {
            closeEp = false
            archiveProfiles = false
            publishReports = false
            publishResults = false
        }
    } else {
        wrapUp(body)
    }

    // raise error in case the regression test had errors
    if (r >= 400) {
        error("Error during regression test (source vs. target config).")
    }
}

// Request String Method

/**
 * Generic method that extracts the properties from the config object and returns the json string to be passed to EP.
 *
 * ToDo: Check if some kind of reflection can be used (right now this method needs to contain all possible properties)
 *      
 *      for field in config.fields:
 *          reqString += '"' + field.name + '": "' + field.value + '", '
 *          
 *      + special handling for paths and some others
 */
def createReqString(config, methodName) {
    if (config instanceof Closure) {
        printToConsole('The step parameters could not be resolved and will therefore be ignored. This can be caused by null pointers during evaluation (parameter = "${myNullVariable.myField}").')
    }
    if (binding.hasVariable('mode')) { // migration suite scenario
        printToConsole("Running btc.$methodName")
    } else {
        printToConsole("Running btc.$methodName: $config")
    }
    def reqString = '{ '
    // Profile
    if (config.profilePath != null) {
        reqString += '"profilePath": "' + toAbsPath("${config.profilePath}") + '", '
        profilePath = toAbsPath("${config.profilePath}")
        exportPath = toAbsPath(getParentDir("${config.profilePath}") + "/reports")
    }
    if (config.uniqueName != null)
        reqString += '"uniqueName": "' + "${config.uniqueName}" + '", '
    if (config.tlModelPath != null)
        reqString += '"tlModelPath": "' + toAbsPath("${config.tlModelPath}") + '", '
    if (config.tlScriptPath != null)
        reqString += '"tlScriptPath": "' + toAbsPath("${config.tlScriptPath}") + '", '
    if (config.startupScriptPath != null)
        reqString += '"startupScriptPath": "' + toAbsPath("${config.startupScriptPath}") + '", '
    if (config.reuseExistingCode != null)
        reqString += '"reuseExistingCode": "' + "${config.reuseExistingCode}" + '", '
    if (config.pilConfig != null)
        reqString += '"pilConfig": "' + "${config.pilConfig}" + '", '
    if (config.pilTimeout != null)
        reqString += '"pilTimeout": "' + "${config.pilTimeout}" + '", '
    if (config.testMode != null)
        reqString += '"testMode": "' + "${config.testMode}" + '", '
    if (config.calibrationHandling != null)
        reqString += '"calibrationHandling": "' + "${config.calibrationHandling}" + '", '
    if (config.environmentXmlPath != null)
        reqString += '"environmentXmlPath": "' + toAbsPath("${config.environmentXmlPath}") + '", '
    if (config.slModelPath != null)
        reqString += '"slModelPath": "' + toAbsPath("${config.slModelPath}") + '", '
    if (config.slScriptPath != null)
        reqString += '"slScriptPath": "' + toAbsPath("${config.slScriptPath}") + '", '
    if (config.addModelInfoPath != null)
        reqString += '"addModelInfoPath": "' + toAbsPath("${config.addModelInfoPath}") + '", '
    if (config.mappingFilePath != null)
        reqString += '"mappingFilePath": "' + toAbsPath("${config.mappingFilePath}") + '", '
    if (config.matlabVersion != null)
        reqString += '"matlabVersion": "' + "${config.matlabVersion}" + '", '
    if (config.matlabInstancePolicy != null)
        reqString += '"matlabInstancePolicy": "' + "${config.matlabInstancePolicy}" + '", '
    if (config.codeModelPath != null)
        reqString += '"codeModelPath": "' + toAbsPath("${config.codeModelPath}") + '", '
    if (config.tlSubsystem != null)
        reqString += '"tlSubsystem": "' + "${config.tlSubsystem}" + '", '
    if (config.tlCalibrationFilter != null)
        reqString += '"tlCalibrationFilter": "' + "${config.tlCalibrationFilter}" + '", '
    if (config.tlSubsystemFilter != null)
        reqString += '"tlSubsystemFilter": "' + "${config.tlSubsystemFilter}" + '", '
    if (config.tlCodeFileFilter != null)
        reqString += '"tlCodeFileFilter": "' + "${config.tlCodeFileFilter}" + '", '
    if (config.compilerShortName != null)
        reqString += '"compilerShortName": "' + "${config.compilerShortName}" + '", '
    if (config.licenseLocationString != null)
        reqString += '"licenseLocationString": "' + "${config.licenseLocationString}" + '", '
    if (config.exportPath != null) {
        exportPath = toAbsPath("${config.exportPath}")
        reqString += '"exportPath": "' + exportPath + '", '
    }
    if (config.loadReportData != null)
        reqString += '"loadReportData": "' + "${config.loadReportData}" + '", '
    if (config.updateRequired != null)    
        reqString += '"updateRequired": "' + "${config.updateRequired}" + '", '
    if (config.disableUpdate != null)    
        reqString += '"disableUpdate": "' + "${config.disableUpdate}" + '", '
    if (config.enableEC != null)    
        reqString += '"enableEC": "' + "${config.enableEC}" + '", '
    if (config.createWrapperModel != null)    
        reqString += '"createWrapperModel": "' + "${config.createWrapperModel}" + '", '
    if (config.saveProfileAfterEachStep != null)
        reqString += '"saveProfileAfterEachStep": "' + "${config.saveProfileAfterEachStep}" + '", '
    if (config.logFilePath != null)
        reqString += '"logFilePath": "' + toAbsPath("${config.logFilePath}") + '", '
    
    // RBT Execution / Regression Test / Formal Test / Back-to-Back
    if (config.executionConfigString != null)
        reqString += '"executionConfigString": "' + "${config.executionConfigString}" + '", '
    if (config.debugConfigString != null) {
        reqString += '"debugConfigString": "' + "${config.debugConfigString}" + '", '
        isDebug = true
    }
    if (config.reportSource != null)
        reqString += '"reportSource": "' + "${config.reportSource}" + '", '
    if (config.reference != null)
        reqString += '"reference": "' + "${config.reference}" + '", '
    if (config.comparison != null)
        reqString += '"comparison": "' + "${config.comparison}" + '", '
    if (config.requirementsWhitelist != null)
        reqString += '"requirementsWhitelist": "' + "${config.requirementsWhitelist}" + '", '
    if (config.requirementsBlacklist != null)
        reqString += '"requirementsBlacklist": "' + "${config.requirementsBlacklist}" + '", '    
    if (config.scopesWhitelist != null)
        reqString += '"scopesWhitelist": "' + "${config.scopesWhitelist}" + '", '
    if (config.scopesBlacklist != null)
        reqString += '"scopesBlacklist": "' + "${config.scopesBlacklist}" + '", '
    if (config.foldersWhitelist != null)
        reqString += '"foldersWhitelist": "' + "${config.foldersWhitelist}" + '", '
    if (config.foldersBlacklist != null)
        reqString += '"foldersBlacklist": "' + "${config.foldersBlacklist}" + '", '
    if (config.testCasesWhitelist != null)
        reqString += '"testCasesWhitelist": "' + "${config.testCasesWhitelist}" + '", '
    if (config.testCasesBlacklist != null)
        reqString += '"testCasesBlacklist": "' + "${config.testCasesBlacklist}" + '", '
    
    
    // Vector Generation
    if (config.analyzeScopesHierachically != null)
        reqString += '"analyzeScopesHierachically": "' + "${config.analyzeScopesHierachically}" + '", '
    if (config.allowDenormalizedFloats != null)
        reqString += '"allowDenormalizedFloats": "' + "${config.allowDenormalizedFloats}" + '", '
    if (config.scope != null)
        reqString += '"scope": "' + "${config.scope}" + '", '
    if (config.pll != null)
        reqString += '"pll": "' + "${config.pll}" + '", '
    if (config.engine != null)
        reqString += '"engine": "' + "${config.engine}" + '", '
    if (config.perPropertyTimeout != null)
        reqString += '"perPropertyTimeout": "' + "${config.perPropertyTimeout}" + '", '
    if (config.globalTimeout != null)
        reqString += '"globalTimeout": "' + "${config.globalTimeout}" + '", '
    if (config.engineTimeout != null)
        reqString += '"engineTimeout": "' + "${config.engineTimeout}" + '", '
    if (config.scopeTimeout != null)
        reqString += '"scopeTimeout": "' + "${config.scopeTimeout}" + '", '
    if (config.considerSubscopes != null)
        reqString += '"considerSubscopes": "' + "${config.considerSubscopes}" + '", '
    if (config.recheckUnreachable != null)
        reqString += '"recheckUnreachable": "' + "${config.recheckUnreachable}" + '", '
    if (config.depthCv != null)
        reqString += '"depthCv": "' + "${config.depthCv}" + '", '
    if (config.depthAtg != null)
        reqString += '"depthAtg": "' + "${config.depthAtg}" + '", '
    if (config.loopUnroll != null)
        reqString += '"loopUnroll": "' + "${config.loopUnroll}" + '", '
    if (config.robustnessTestFailure != null)
        reqString += '"robustnessTestFailure": "' + "${config.robustnessTestFailure}" + '", '
    if (config.inputRestrictions != null)
        reqString += '"inputRestrictions": "' + toAbsPath("${config.inputRestrictions}") + '", '
    if (config.createReport != null)
        reqString += '"createReport": "' + "${config.createReport}" + '", '
    if (config.numberOfThreads != null)
        reqString += '"numberOfThreads": "' + "${config.numberOfThreads}" + '", '
    if (config.parallelExecutionMode != null)
        reqString += '"parallelExecutionMode": "' + "${config.parallelExecutionMode}" + '", '
    
    // Formal Verification
    if (config.searchDepth != null)
        reqString += '"searchDepth": "' + "${config.searchDepth}" + '", '
    if (config.memoryLimit != null)
        reqString += '"memoryLimit": "' + "${config.memoryLimit}" + '", '
    if (config.timeLimit != null)
        reqString += '"timeLimit": "' + "${config.timeLimit}" + '", '
    
    // Domain Coverage + Range Violation
    if (config.scopePath != null)
        reqString += '"scopePath": "' + "${config.scopePath}" + '", '
    if (config.rvXmlPath != null)
        reqString += '"rvXmlPath": "' + toAbsPath("${config.rvXmlPath}") + '", '
    if (config.considerOutputs != null)
        reqString += '"considerOutputs": "' + "${config.considerOutputs}" + '", '
    if (config.considerLocals != null)
        reqString += '"considerLocals": "' + "${config.considerLocals}" + '", '
    if (config.checkRangeSpecification != null)
        reqString += '"checkRangeSpecification": "' + "${config.checkRangeSpecification}" + '", '
    if (config.dcXmlPath != null)
        reqString += '"dcXmlPath": "' + toAbsPath("${config.dcXmlPath}") + '", '
    if (config.raster != null)
        reqString += '"raster": "' + "${config.raster}" + '", '
    if (config.activateRangeViolationCheck != null)
        reqString += '"activateRangeViolationCheck": "' + "${config.activateRangeViolationCheck}" + '", '
    if (config.activateBoundaryCheck != null)
        reqString += '"activateBoundaryCheck": "' + "${config.activateBoundaryCheck}" + '", '
    if (config.addDomainBoundaryForInputs != null)
        reqString += '"addDomainBoundaryForInputs": "' + "${config.addDomainBoundaryForInputs}" + '", '
    
    // UDCG
    if (config.valueRegions != null)
        reqString += '"valueRegions": "' + "${config.valueRegions}" + '", '
    if (config.scopeRegex != null)
        reqString += '"scopeRegex": "' + "${config.scopeRegex}" + '", '

    // Vector Import/Export
    if (config.importDir != null)
        reqString += '"importDir": "' + toAbsPath("${config.importDir}") + '", '
    if (config.exportDir != null)
        reqString += '"exportDir": "' + toAbsPath("${config.exportDir}") + '", '
    if (config.vectorFormat != null)
        reqString += '"vectorFormat": "' + "${config.vectorFormat}" + '", '
    if (config.vectorKind != null)
        reqString += '"vectorKind": "' + "${config.vectorKind}" + '", '
    
    // Tolerance Import & Code Analysis Report
    if (config.path != null)
        reqString += '"path": "' + toAbsPath("${config.path}") + '", '
    if (config.useCase != null)
        reqString += '"useCase": "' + "${config.useCase}" + '", '
    if (config.applyTo != null)
        reqString += '"applyTo": "' + "${config.applyTo}" + '", '
    if (config.relTolerance != null)
        reqString += '"relTolerance": "' + "${config.relTolerance}" + '", '
    if (config.absToleranceFlp != null)
        reqString += '"absToleranceFlp": "' + "${config.absToleranceFlp}" + '", '
    if (config.absToleranceFxp != null)
        reqString += '"absToleranceFxp": "' + "${config.absToleranceFxp}" + '", '
    if (config.fxpIsMultipleOfLsb != null)
        reqString += '"fxpIsMultipleOfLsb": "' + "${config.fxpIsMultipleOfLsb}" + '", '
    
    // Execution Record Import / Export + Model Coverage Report
    if (config.isMigration != null)
        reqString += '"isMigration": "' + "${config.isMigration}" + '", '
    if (config.dir != null)
        reqString += '"dir": "' + toAbsPath("${config.dir}") + '", '
    if (config.executionConfig != null)
        reqString += '"executionConfig": "' + "${config.executionConfig}" + '", '
    if (config.exportFormat != null)
        reqString += '"exportFormat": "' + "${config.exportFormat}" + '", '
    // CodeAnalysisReport
    if (config.includeSourceCode != null)
        reqString += '"includeSourceCode": "' + "${config.includeSourceCode}" + '", '
    if (config.reportName != null)
        reqString += '"reportName": "' + "${config.reportName}" + '", '
    // default tolerances
    if (config.configFilePath != null)
        reqString += '"configFilePath": "' + toAbsPath("${config.configFilePath}") + '", '
    if (config.tolerancesFilePath != null)
        reqString += '"tolerancesFilePath": "' + toAbsPath("${config.tolerancesFilePath}") + '", '
    if (config.variant != null)
        reqString += '"variant": "' + "${config.variant}" + '", '
    // Interface Report
    if (config.scopeNameRegex != null)
        reqString += '"scopeNameRegex": "' + "${config.scopeNameRegex}" + '", '
    // Remove Incompatible Vectors
    if (config.checkRanges != null)
        reqString += '"checkRanges": "' + "${config.checkRanges}" + '", '
    if (config.checkResolution != null)
        reqString += '"checkResolution": "' + "${config.checkResolution}" + '", '
    if (config.checkTestCases != null)
        reqString += '"checkTestCases": "' + "${config.checkTestCases}" + '", '


    reqString = reqString.trim()
    if (reqString.endsWith(','))
        reqString = reqString.substring(0, reqString.length() - 1)
    reqString += ' }'
    return reqString
}

// Utility methods

def getPort() {
    port
}

def getReportPath() {
    exportPath
}

/**
 * Resolves unresolved closures (groovy magic)
 */
def resolveConfig(body) {
    def config = [:]
    try {
        body.resolveStrategy = Closure.DELEGATE_ONLY
        body.delegate = config
        body()
    } catch (err) {
        config = body
    }
    return config
}

/**
 * Returns an absolute path. If the input is a relative path it is resolved
 * to the current directory using the Jenkins Pipeline command pwd().
 * In all cases backslashes are replaced by slashes for compatibility reasons.
 */
def toAbsPath(path) {
    // replace backslashes with slashes, they're easier to work with
    def sPath = path.replace("\\", "/")
    // replace multiple occurrences: e.g. C://file -> C:/file
    sPath = sPath.replaceAll("(/)+", "/")
    if (sPath.startsWith("/"))
        sPath = sPath.substring(1, sPath.length())
    if (sPath.contains(":"))
        return sPath
    def wd = pwd().replace("\\", "/")
    wd = wd.replaceAll("(/)+", "/")
    return wd + "/" + sPath
}

/**
 * Converts an absolute path into a path relative to pwd (if the path points to a location on or below pwd).
 * Backslashes are replaced by slashes for compatibility reasons.
 */
def toRelPath(path) {
    // replace backslashes with slashes, they're easier to work with
    def sPath = path.replace("\\", "/")
    // replace multiple occurrences: e.g. C://file -> C:/file
    sPath = sPath.replaceAll("(/)+", "/")
    def wd = pwd().replace("\\", "/")
    wd = wd.replaceAll("(/)+", "/")
    if (sPath.contains(wd)) {
        return sPath.replace(wd + "/", "")
    } else {
        return sPath
    }    
}

def getParentDir(path) {
    path = path.trim()
    if (path.startsWith("/"))
        path = path.substring(1, path.length())
    if (path.endsWith("/"))
        path = path.substring(0, path.length() - 1)
    if (!path.contains("/"))
        return ""
    def parentDir = path.substring(0, path.lastIndexOf("/"))
    if (parentDir.contains(":")) {
        return parentDir
    } else {
        return "/" + parentDir
    }
}

/**
 * Takes the epInstallDir and returns the first jre directory it finds
 */
def getJreDir(epInstallDir) {
    def output = bat returnStdout: true, script: "dir \"${epInstallDir}/jres\" /b /A:D"
    def foldersList = output.trim().tokenize('\n').collect() { it }
    def jdkFolder = null
    def jreFolder = null
    for (folder in foldersList) {
        if (folder.startsWith('jdk')) {
            jdkFolder = epInstallDir + '/jres/' + folder.trim() + '/bin'
            break // no need to search further, jdk folder is preferred
        }
        if (folder.startsWith('jre')) {
            jreFolder = epInstallDir + '/jres/' + folder.trim() + '/bin'
        }
    }
    if (jdkFolder == null) {
        return jreFolder
    } else {
        return jdkFolder
    }
    
}

def initProjectOverview() {
    projectOverviewList = []
}


/**
 * Utility method to query available execution configs
 *
 * Returns an unsorted collection of executionConfigs (Strings).
 */
def getAvailableExecutionConfigs() {
    def cfgs = httpRequest quiet: true, httpMode: 'GET', url: "http://localhost:${epJenkinsPort}/getAvailableExecutionConfigs", validResponseCodes: '100:500'
    return cfgs
}

/**
 * Utility method to query a status summary struct
 * Returns a struct with information about the profile:
 *  - ProfileName
 *  - Coverage
 *    - RBT
 *      - RequirementsCoverage
 *      - StatementCoverage
 *      - ConditionCoverage
 *      - DecisionCoverage
 *      - Condition_DecisionCoverage
 *      - RelationalOperatorCoverage
 *      - FunctionCoverage
 *      - FunctionCallCoverage
 *      - SwitchCaseCoverage
 *      - DivisionByZeroRobustnessCheck
 *      - DowncastRobustnessCheck
 *    - B2B
 *      - StatementCoverage
 *      - ConditionCoverage
 *      - DecisionCoverage
 *      - Condition_DecisionCoverage
 *      - RelationalOperatorCoverage
 *      - FunctionCoverage
 *      - FunctionCallCoverage
 *      - SwitchCaseCoverage
 *      - DivisionByZeroRobustnessCheck
 *      - DowncastRobustnessCheck
 *  - TestCases
 *    - [List of objects]
 *      - Name
 *      - Description
 *      - Result
 *      - VerifiedRequirements
 *          - [List of Strings]
 *      - Length
 *      - CreatedOn
 *      - CreatedBy
 */
def getStatusSummary() {
    def r = httpRequest quiet: true, httpMode: 'GET', url: "http://localhost:${epJenkinsPort}/getStatusSummary", validResponseCodes: '100:500'
    return r.content
}

/**
 * Utility method to query available execution configs
 *
 * Returns an unsorted collection of executionConfigs (Strings).
 */
def checkPlugin(qualifierOrListOfQualifiers) {
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: qualifierOrListOfQualifiers, url: "http://localhost:${epJenkinsPort}/checkPlugin", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
}

def getCallingMethodName(){
  def marker = new Exception()
  return StackTraceUtils.sanitize(marker).stackTrace[2].methodName
}

def printToConsole(string) {
    echo "[BTC] " + string
}


/**
 * Returns true if EP is available at the current epJenkinsPort, false otherwise
 */
def isEpAvailable() {
    def r = httpRequest quiet: true, url: "http://localhost:${epJenkinsPort}/check", validResponseCodes: '100:500'
    return r.status == 200
}


/**
 * Finds the next available port, registers it in the bt-port-registry and returns it.
 *
 * - queries the btc-port-registry to find the next avaialble port
 * - finds the next available port based on the given port number
 * - updates the btc-port-registry to reflect that the port is in use
 */
def findNextAvailablePort(portString) {
    def appDataDir = "%AppData%\\BTC\\JenkinsAutomation"
    def lockFile = "${appDataDir}\\.lock"
    def portRegistryName = ".port-registry"
    def portRegistryFile = "${appDataDir}\\${portRegistryName}"
    try {

        bat label: 'Query port registry', returnStdout: true, script:
        """
        @echo off
        SET count=0

        :CHECK
        IF NOT EXIST "$appDataDir" MKDIR "$appDataDir"
        MKDIR "$lockFile" 2> NUL || GOTO FAIL
        IF EXIST "$portRegistryFile" (
            COPY /Y "$portRegistryFile" $portRegistryName
        ) ELSE (
            COPY /y NUL .port-registry >NUL
        )
        GOTO END

        :FAIL
        SET /A count+=1
        PING localhost -n 2 >NUL
        IF %count% GEQ 10 RMDIR /S /Q "$lockFile"
        GOTO CHECK

        :END
        """
    } catch (err) {
        error("Could not get lock on btc-port-registry: " + err)
    }
    
    // key: port, value: process id
    portMap = readYaml file: '.port-registry'
    if (!(portMap instanceof Map)) {
        portMap = [:]
    }
    def epJenkinsPortNumber = Integer.parseInt("" + portString)
    def listedProcess = portMap[epJenkinsPortNumber]
    while (listedProcess != null) {
        if (isProcessAlive(listedProcess)) {
            epJenkinsPortNumber++
            listedProcess = portMap[epJenkinsPortNumber]
        } else {
            listedProcess = null
        }
    }
    return epJenkinsPortNumber
}

def reservePortAndReleaseRegistryLock(epJenkinsPortNumber) {
    portMap[epJenkinsPortNumber] = PID  // put current process into the map
    writeYaml file: '.port-registry', data: portMap, overwrite: true
    updatePortRegistry()
}

def updatePortRegistry() {
    def appDataDir = "%AppData%\\BTC\\JenkinsAutomation"
    def lockFile = "${appDataDir}\\.lock"
    try {
        bat label: 'Update port registry', returnStdout: true, script:
        """
        @echo off
        COPY /Y .port-registry "%AppData%\\BTC\\JenkinsAutomation\\.port-registry"
        RMDIR /S /Q "$lockFile"
        DEL ".port-registry"
        """
    } catch (err) {
        error("Unable to update btc-port-registry: " + err)
    }
}

boolean isProcessAlive(pid) {
    def out = bat script: """@echo off
    tasklist /fi \"pid eq ${pid}\"""", returnStdout: true
    return out.trim().length() > 200
}

def toJson(object) {
    writeJSON file: 'temp1906235690.json', json: object
    def rJSON = readFile encoding: 'utf-8', file: 'temp1906235690.json'
    def ignore = bat returnStdout: true, script: 'del /f /q temp1906235690.json'
    return rJSON
}

/**
 * Releases the used port by removing it from the bt-port-registry.
 */
def releasePort(port) {
    def appDataDir = "%AppData%\\BTC\\JenkinsAutomation"
    def lockFile = "${appDataDir}\\.lock"
    def portRegistryName = ".port-registry"
    def portRegistryFile = "${appDataDir}\\${portRegistryName}"
    try {
        bat label: 'Query port registry', returnStdout: true, script:
        """
        @echo off
        SET count=0

        :CHECK
        IF NOT EXIST "$appDataDir" MKDIR "$appDataDir"
        MKDIR "$lockFile" 2> NUL|| GOTO FAIL
        IF EXIST "$portRegistryFile" (
            COPY /Y "$portRegistryFile" $portRegistryName
        ) ELSE (
            COPY /y NUL .port-registry >NUL
        )
        GOTO END
        
        :FAIL
        IF %count% GEQ 10 EXIT -1
        SET /A count+=1
        PING localhost -n 2 >NUL
        GOTO CHECK

        :END
        """
    } catch (err) {
        error("Could not get lock on btc-port-registry: " + err)
    }    
    def portMap = readYaml file: '.port-registry'
    if (!(portMap instanceof Map)) {
        return
    }
    def epJenkinsPortNumber = Integer.parseInt("" + port)
    portMap.remove(epJenkinsPortNumber)
    writeYaml file: '.port-registry', data: portMap, overwrite: true    
    updatePortRegistry()
}
