package dsl
// vars/btc.groovy

/**
 * Special method to start the ep process and connect to it,
 * when EP is running in a linux container
 */
def startup() {
    sh('#!/bin/sh -e\n' + 'echo "${EP_INSTALL_PATH}/ep -clearPersistedState -application ep.application.headless -nosplash -vmargs -Dep.runtime.batch=ep -Dep.linux.config=${EP_REGISTRY} -Dlogback.configurationFile=${EP_LOG_CONFIG} -Dep.configuration.logpath=${WORKSPACE}/logs -Dep.runtime.workdir=${WORK_DIR} -Dct.root.temp.dir=${TMP_DIR} -Dep.licensing.location=${LICENSE_LOCATION} -Dep.licensing.package=${ET_COMPLETE} -Dep.rest.port=8080" > startEP.sh && chmod +x startEP.sh && ./startEP.sh > /dev/null 2>&1 &')
    def ip = sh(returnStdout: true, script: '#!/bin/sh -e\n' + 'hostname -I | awk "{print $1}"').trim()
    btcStart(simplyConnect: true, ipAddress: ip)
}

/*
 * ##################################################################
 *
 *          METHODS FOR BACKWARDS COMPATIBILITY
 *
 * ##################################################################
 */

def vectorImport(body) {
    // transform the closure into a key value structure
    def config = resolveConfig(body)
    // pass the structure to the corresponding step
    return btcVectorImport(config)
}

// TODO: tlSubsystem argument is missing from the function itself
// TODO: calibrationHandling argument is missing from the function itself
// TODO: testMode, reuseExistingCode, logFilePath also missing
def profileLoad(body) {
	def config = resolveConfig(body)
	return btcProfileLoad(config) 
}
// TODO: tlSubsystemFilter, tlCalibrationFilter, tlCodeFileFilter replaced by subsystemMatcher string
// TODO: from profileLoad, missing startupScriptPath, codeModelPath, slModelPath, slScriptPath, addModelInfoPath, pilTimeout, exportPath, updateRequired, logFilePath
def profileCreateTL(body) {
	def config = resolveConfig(body)
	return btcProfileCreateTL(config) 
}

// TODO: inheritance from profileLoad is missing: environmentXmlPath, codeModelPath, addModelInfoPath, pilConfig, pilTimeout, reuseExistingCode, updateRequired, logFilePath
// TODO: compilerShortName, codeModelPath, mappingFilePath missing 
def profileCreateEC(body) {
	def config = resolveConfig(body)
	if (config.calibrationHandling == "EXPLICIT PARAM") {
		config.parameterHandling = "EXPLICIT PARAMETER"
	} else if (config.calibrationHandling == "OFF") {
		config.parameterHandling = "OFF"
	} else if (config.calibrationHandling == "LIMITED BLOCKSET" {
		config.parameterHandling = "LIMITED BLOCKSET"
	} else {
	// TODO: throw error, invalid calibrationHandling
	}
	return btcProfileCreateEC(config) 
}

// TODO: missing addModelInfoPath
// TODO: inheritance from profileLoad is missing: environmentXmlPath, codeModelPath, addModelInfoPath, pilConfig, pilTimeout, calibrationHandling, testMode, updateRequired, logFilePath
def profileCreateSL(body) {
	def config = resolveConfig(body)
	return btcProfileCreateSL(config) 
}

// TODO: inheritance from profileLoad is missing: environmentXmlPath, pilConfig, pilTimeout, logFilePath, updateRequired
def profileCreateC(body) {
	def config = resolveConfig(body)
	return btcProfileCreateC(config) 
}


def wrapUp(body) {
	def config = resolveConfig(body)
	return btcWrapUp(config) 
}

// TODO: this entire function is unwritten
def executionRecordExport(body) {
	def config = resolveConfig(body)
	return btcExecutionRecordExport(config) 
}

// TODO: this entire function is unwritten
def executionRecordImport(body) {
	def config = resolveConfig(body)
	return btcExecutionRecordImport(config) 
}

// TODO: this entire function is unwritten
def inputRestrictionsExport(body) {
	def config = resolveConfig(body)
	return btcInputRestrictionsExport(config) 
}

// TODO: this entire function is unwritten
def inputRestrictionsImport(body) {
	def config = resolveConfig(body)
	return btcInputRestrictionsImport(config) 
}

// TODO: this entire function is unwritten
def toleranceExport(body) {
	def config = resolveConfig(body)
	return btcToleranceExport(config) 
}

// TODO: this entire function is unwritten
def toleranceImport(body) {
	def config = resolveConfig(body)
	return btcToleranceImport(config) 
}

def vectorExport(body) {
	def config = resolveConfig(body)
	return btcVectorExport(config) 
}

def vectorImport(body) {
	def config = resolveConfig(body)
	return btcVectorImport(config) 
}

// TODO: this entire function is undefined
def addInputCombinationGoals(body) {
	def config = resolveConfig(body)
	return btcAddInputCombinationGoals(config) 
}

def backToBack(body) {
	def config = resolveConfig(body)
	return btcBackToBack(config) 
}

def codeAnalysisReport(body) {
	def config = resolveConfig(body)
	return btcCodeAnalysisReport(config) 
}


def rbtExecution(body) {
	def config = resolveConfig(body)
	return btcRbtExecution(config) 
}


def vectorGeneration(body) {
	def config = resolveConfig(body)
	config.propertyTimeout = config.perPropertyTimeout
	return btcVectorGeneration(config) 
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
