package dsl
// vars/btc.groovy

def epJenkinsPort = null       // the port to use for communication with EP
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
    def r = btcStart(config)
    return r
}

def handleError(e) {
    try { wrapUp {} } catch (err) {} finally { error(e) }
}

// Profile Creation steps

def profileLoad(body) {
    def config = resolveConfig(body)
    return btcProfileLoad(config)
}

def profileCreateTL(body) {
    def config = resolveConfig(body)
    return btcProfileCreateTL(config)
}

def profileCreateEC(body) {
    def config = resolveConfig(body)
    return btcProfileCreateEC(config)
}

def profileCreateSL(body) {
    def config = resolveConfig(body)
    return btcProfileCreateSL(config)
}

def profileCreateC(body) {
    def config = resolveConfig(body)
    return btcProfileCreateC(config)
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
    if (r.status == 300) {
        unstable('Tests failed.')
    }
    return r.status
}

def formalTest(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'formalTest')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/formalTest", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    if (r.status == 300) {
        unstable('FormalTest failed.')
    }
    return r.status
}

def vectorGeneration(body) {
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    return btcVectorGeneration(config)
}

def backToBack(body) {
    def config = resolveConfig(body)
    return btcBackToBack(config)
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
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'rangeViolationGoals')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addRangeViolationGoals", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
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
    // evaluate the body block, and collect configuration into the object
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'domainCoverageGoals')
    // call EP to invoke test execution
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/addDomainCoverageGoals", validResponseCodes: '100:500'
    printToConsole(" -> (${r.status}) ${r.content}")
    return r.status
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

def setDefaultTolerances(body) {
    // call EP
    def config = resolveConfig(body)
    def reqString = createReqString(config, 'setDefaultTolerances')
    def r = httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/setDefaultTolerances", validResponseCodes: '100:500'
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
    return btcWrapUp(config)
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
    
    // dispatch args
    migrationTmpDir = toAbsPath("MigrationTmp")
    config.migrationTmpDir = toAbsPath(migrationTmpDir)
    config.exportPath = migrationTmpDir + "/reports"
    config.saveProfileAfterEachStep = true
    
    // Startup
    startup(body)
    // Profile Creation
    if (config.profilePath == null) {
        // create a new profile
        config.profilePath = migrationTmpDir + "/profiles/profile.epp"
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
        httpRequest quiet: true, httpMode: 'POST', requestBody: reqString, url: "http://localhost:${epJenkinsPort}/kill", validResponseCodes: '100:500'
    } catch (err) {
        printToConsole('BTC EmbeddedPlatform successfully closed.')
    } finally {
        releasePort(epJenkinsPort)
    }
    def relativeMigTmpDir = toRelPath(migrationTmpDir)
    def relativeExportPath = toRelPath(exportPath)
    archiveArtifacts allowEmptyArchive: true, artifacts: "${relativeMigTmpDir}/profiles/*.epp"
    printToConsole("stashing files: ${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/*")
    stash includes: "${relativeMigTmpDir}/er/**/*.mdf, ${relativeExportPath}/**/*", name: "migration-source"
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
    } else if (config.codeModelPath != null) {
        r = profileCreateC(config)
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
def createReqString(config, methodName) {
    printToConsole("Running btc.$methodName: $config")
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
    if (config.addDomainBoundaryForInputs != null)
        reqString += '"addDomainBoundaryForInputs": "' + "${config.addDomainBoundaryForInputs}" + '", '
    
    // UDCG
    if (config.valueRegions != null)
        reqString += '"valueRegions": "' + "${config.valueRegions}" + '", '

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
        IF %count% GEQ 15 RMDIR /S /Q "$lockFile"
        GOTO CHECK

        :END
        """
    } catch (err) {
        error("Could not get lock on btc-port-registry: " + err)
    }
    
    def portList = readYaml file: '.port-registry'
    //Collections.sort(portList) // sort list to make sure port numbers are ascending
    portList.sort()
    def epJenkinsPortNumber = Integer.parseInt("" + portString)
    for (port in portList) {
        if (port == epJenkinsPortNumber) {
            epJenkinsPortNumber++
        } else if (port > epJenkinsPortNumber) {
            // sorted list ensures that no other ports collide with this one
            break
        }
    }
    portList += epJenkinsPortNumber
    writeYaml file: '.port-registry', data: portList, overwrite: true
    
    try {
        bat label: 'Update port registry', returnStdout: true, script:
        """
        @echo off
        COPY /Y .port-registry "%AppData%\\BTC\\JenkinsAutomation\\.port-registry"
        RMDIR /S /Q "$lockFile"
        """
    } catch (err) {
        error("Unable to update btc-port-registry: " + err)
    }

    return epJenkinsPortNumber
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
        IF %count% GEQ 15 EXIT -1
        SET /A count+=1
        PING localhost -n 2 >NUL
        GOTO CHECK

        :END
        """
    } catch (err) {
        error("Could not get lock on btc-port-registry: " + err)
    }    
    def portList = readYaml file: '.port-registry'
    def epJenkinsPortNumber = Integer.parseInt("" + port)
    portList -= epJenkinsPortNumber
    writeYaml file: '.port-registry', data: portList, overwrite: true    
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
