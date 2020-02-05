package dsl
// vars/btc.groovy

def restPort = null       // the port to use for communication with EP
def isDebug  = null       // specifies if a debug environment should be exported and archived (true, false)
def mode     = null       // mode for migration suite (source, target)

/**
 * Tries to connect to EP using the default port 29267 (unless specified differently).
 * If EP is not available a new instance is started via a batch command. Availability
 * is then checked until success or until the timeout (default: 2 minutes) has expired.
 */
def startup(body = {}) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    if (config.port != null) {
        restPort = config.port
    } else {
        restPort = "29267" // default as fallback
    }
    def timeoutSeconds = 120
    if (config.timeout != null)
        timeoutSeconds = config.timeout
    
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
    def epVersion = null
    try {
        def split = "${epInstallDir}".split("\\\\")
        epVersion = split[split.length - 1].substring(2)
        echo "Connecting to EP ${epVersion}... (timeout: " + timeoutSeconds + " seconds)"
    } catch (err) {
        error("Invalid path to BTC EmbeddedPlatform installation: ${epInstallDir}")
    }
    
    // start EP and wait for it to be available at the given port
    def r = httpRequest quiet: true, url: "http://localhost:${restPort}/check", validResponseCodes: '100:500'
    def responseCode = r.status
    if (r.status == 200) {
        echo "(201) Successfully connected to an running instance of BTC EmbeddedPlatform on port: ${restPort}."
        responseCode = 201 // connected to an existing instance
    } else { // try to connect to EP until timeout is reached
        // configure license packages (required since 2.4.0 to support ET_BASE)
        def licensingPackage = config.licensingPackage
        if (licensingPackage == null) {
            licensingPackage = 'ET_COMPLETE,ET_AUTOMATION_SERVER'
        } else if (licensingPackage.equals('ET_COMPLETE')) {
            licensingPackage += ',ET_AUTOMATION_SERVER'
        } else if (licensingPackage.equals('ET_BASE')) {
            licensingPackage += ',ET_AUTOMATION_SERVER_BASE'
        }
        def epPort = getEPPort(restPort)
        timeout(time: timeoutSeconds, unit: 'SECONDS') { // timeout for connection to EP
            try {
                epJreDir = getJreDir(epInstallDir)
                epJreString = ''
                if (epJreDir != null) {
                    epJreString = ' -vm "' + epJreDir + '"'
                }
                bat label: 'Starting BTC EmbeddedPlatform', script: "start \"\" \"${epInstallDir}/rcp/ep.exe\" -clearPersistedState \
                -application com.btc.ep.application.headless -nosplash${epJreString} \
                -vmargs -Dep.runtime.batch=com.btc.ep -Dep.runtime.api.port=${epPort} \
                -Dosgi.configuration.area.default=\"%USERPROFILE%/AppData/Roaming/BTC/ep/${epVersion}/${restPort}/configuration\" \
                -Dosgi.instance.area.default=\"%USERPROFILE%/AppData/Roaming/BTC/ep/${epVersion}/${restPort}/workspace\" \
                -Dep.configuration.logpath=AppData/Roaming/BTC/ep/${epVersion}/${restPort}/logs -Dep.runtime.workdir=BTC/ep/${epVersion}/${restPort} \
                -Dep.licensing.package=${licensingPackage} -Dep.rest.port=${restPort}"
                waitUntil {
                    r = httpRequest quiet: true, url: "http://localhost:${restPort}/check", validResponseCodes: '100:500'
                    // exit waitUntil closure
                    return (r.status == 200)
                }
                echo "(200) Successfully started and connected to BTC EmbeddedPlatform ${epVersion} on port: ${restPort}."
                responseCode = 200 // connected to a new instance
            } catch(err) {
                error("(400) Connection attempt to BTC EmbeddedPlatform timed out after " + timeoutSeconds + " seconds.")
            }
        }
    }
    return responseCode
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
    def reqString = createReqString(config)
    echo "$config"
    // call EP to invoke profile creation / loading / update
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/${method}", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    isDebug = false
    if (r.status >= 400) {
        wrapUp {
            publishResults = false
        }
        def relativeReportPath = toRelPath(exportPath)
        publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: "${relativeReportPath}", reportFiles: 'ProfileMessages.html', reportName: 'Profile Messages'])
        error("Error during profile load / creation. Aborting (you cannot continue without a profile).")
    }
    return r.status
}

// Normal Steps

def testExecutionReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/testExecutionReport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def codeAnalysisReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/codeAnalysisReport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def modelCoverageReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/modelCoverageReport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def xmlReport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/xmlReportExport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def rbtExecution(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/testexecution", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def formalTest(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/formalTest", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def vectorGeneration(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke vector generation and analysis
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/vectorGeneration", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def backToBack(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke back-to-back test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/backToBack", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def regressionTest(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/regressionTest", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def rangeViolationGoals(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/addRangeViolationGoals", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def domainCoverageGoals(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/addDomainCoverageGoals", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def vectorImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/vectorImport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def toleranceImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/toleranceImport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def toleranceExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/toleranceExport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def inputRestrictionsImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/inputRestrictionsImport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def inputRestrictionsExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/inputRestrictionsExport", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def formalVerification(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/formalVerification", validResponseCodes: '100:500'
    echo "(${r.status}) ${r.content}"
    return r.status
}

def executionRecordExport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/exportExecutionRecords", validResponseCodes: '100:500'
    return r.status
}

def executionRecordImport(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config)
    // call EP
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/importExecutionRecords", validResponseCodes: '100:500'
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
    publishReports = true
    if (config.publishReports != null)
        publishReports = config.publishReports
    publishResults = true
    if (config.publishResults != null)
        publishResults = config.publishResults
    try {
        // Closes BTC EmbeddedPlatform. Try-Catch is needed because the REST API call
        // will throw an exception as soon as the tool closes. This is expected.
        def reqString = "" // removed closeEp parameter, it was only causing problems
        httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        echo 'BTC EmbeddedPlatform successfully closed.'
    }
    def relativeReportPath = toRelPath(exportPath)
    def profilePathParentDir = getParentDir(toRelPath(profilePath))
    try {
        // archiveArtifacts works with relative paths
        if (archiveProfiles) {
            archiveArtifacts allowEmptyArchive: true, artifacts: "${profilePathParentDir}/*.epp"
        }
        if (isDebug)
            archiveArtifacts allowEmptyArchive: true, artifacts: "${relativeReportPath}/Debug_*.zip"
        // fileExists check needs absolute path
        if (publishReports && fileExists("${exportPath}/TestAutomationReport.html"))
            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: "${relativeReportPath}", reportFiles: 'TestAutomationReport.html', reportName: 'Test Automation Report'])
    } catch (err) {
        echo err.message
    }
    // JUnit works with relative paths
    if (publishResults) {
        echo "Looking for junit results in '" + "${relativeReportPath}/junit-report.xml" + "'"
        junit allowEmptyResults: true, testResults: "${relativeReportPath}/junit-report.xml"
    }
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
    
    // dispatch args
    if (config.matlabVersion == null)
        error("Matlab version for MigrationSource needs to be defined.")
    
    migrationTmpDir = toAbsPath("MigrationTmp")
    config.migrationTmpDir = toAbsPath(migrationTmpDir)
    config.exportPath = migrationTmpDir + "/reports"
    config.profilePath = migrationTmpDir + "/profiles/profile.epp"
    config.saveProfileAfterEachStep = true
    
    // Startup
    startup(body)
    // Profile Creation
    if (config.tlModelPath != null) {
        r = profileCreateTL(config)
    } else if (config.slModelPath != null) {
        r = profileCreateEC(config)
    } else {
        error("Please specify the model to be tested (target configuration).")
    }
    if (r >= 300) {
        wrapUp(body)
        error("Error during profile creation (source)")
    }
    
    // Vector Generation
    vectorGeneration(body)
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
        httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${restPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        echo 'BTC EmbeddedPlatform successfully closed.'
    }
    def relativeMigTmpDir = toRelPath(migrationTmpDir)
    def relativeExportPath = toRelPath(exportPath)
    archiveArtifacts allowEmptyArchive: true, artifacts: "${relativeMigTmpDir}/profiles/*.epp"
    echo "stashing files: ${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/*"
    stash includes: "${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/*", name: "migration-source"
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
    if ("${config.matlabVersion}" == "null")
        error("The matlabVersion for MigrationTarget needs to be defined.")
    
    migrationTmpDir = toAbsPath("MigrationTmp")
    config.migrationTmpDir = toAbsPath(migrationTmpDir)
    config.exportPath = migrationTmpDir + "/reports"
    config.profilePath = migrationTmpDir + "/profiles/profile.epp"
    config.saveProfileAfterEachStep = true
    config.loadReportData = true
    
    // unstash files from source slave
    unstash "migration-source"
    
    // Startup
    startup(body)
    // Profile Creation
    if (config.tlModelPath != null) {
        r = profileCreateTL(config)
    } else if (config.slModelPath != null) {
        r = profileCreateEC(config)
    } else {
        error("Please specify the model to be tested (target configuration).")
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
    if (r >= 400) {
        wrapUp(body)
        error("Error during regression test (source vs. target config).")
    }
    
    // Wrap Up
    wrapUp(body)
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
def createReqString(config) {
    def reqString = '{ '
    // Profile
    if (config.profilePath != null) {
        reqString += '"profilePath": "' + toAbsPath("${config.profilePath}") + '", '
        profilePath = toAbsPath("${config.profilePath}")
        exportPath = toAbsPath(getParentDir("${config.profilePath}") + "/reports")
    }
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
    if (config.mappingXmlPath != null)
        reqString += '"mappingXmlPath": "' + toAbsPath("${config.mappingXmlPath}") + '", '
    if (config.matlabVersion != null)
        reqString += '"matlabVersion": "' + "${config.matlabVersion}" + '", '
    if (config.matlabInstancePolicy != null)
        reqString += '"matlabInstancePolicy": "' + "${config.matlabInstancePolicy}" + '", '
    if (config.codeModelPath != null)
        reqString += '"codeModelPath": "' + toAbsPath("${config.codeModelPath}") + '", '
    if (config.tlSubsystem != null)
        reqString += '"tlSubsystem": "' + "${config.tlSubsystem}" + '", '
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
    if (config.enableEC != null)    
        reqString += '"enableEC": "' + "${config.enableEC}" + '", '
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
    
    // Vector Import
    if (config.importDir != null)
        reqString += '"importDir": "' + toAbsPath("${config.importDir}") + '", '
    if (config.vectorFormat != null)
        reqString += '"vectorFormat": "' + "${config.vectorFormat}" + '", '
    if (config.vectorKind != null)
        reqString += '"vectorKind": "' + "${config.vectorKind}" + '", '
    
    // Tolerance Import & Code Analysis Report
    if (config.path != null)
        reqString += '"path": "' + toAbsPath("${config.path}") + '", '
    if (config.useCase != null)
        reqString += '"useCase": "' + "${config.useCase}" + '", '
    
    // Execution Record Import / Export + Model Coverage Report
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

def getEPPort(restPort) {
    def epPort = 29300 + Integer.parseInt("" + restPort) % 100
    // in case the ports are equal
    if (epPort == Integer.parseInt("" + restPort))
        epPort = epPort - 100
    return epPort
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
    return sPath.replace(wd + "/", "")
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
    return "/" + parentDir
}

/**
 * Takes the epInstallDir and returns the first jre directory it finds
 */
def getJreDir(epInstallDir) {
    def output = bat returnStdout: true, script: "dir \"${epInstallDir}/jres\" /b /A:D"
    def foldersList = output.trim().tokenize('\n').collect() { it }
    for (folder in foldersList) {
        if (folder.startsWith('jre')) {
            return epInstallDir + '/jres/' + folder + '/bin'
        }
    }
    return null
}

/**
 * Utility method to query available execution configs
 *
 * Returns an unsorted collection of executionConfigs (Strings).
 */
def getAvailableExecutionConfigs() {
    cfgs = httpRequest quiet: true, httpMode: 'GET', url: "http://localhost:${restPort}/getAvailableExecutionConfigs", validResponseCodes: '100:500'
    return cfgs
}