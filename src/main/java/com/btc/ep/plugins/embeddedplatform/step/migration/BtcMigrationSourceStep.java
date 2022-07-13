package com.btc.ep.plugins.embeddedplatform.step.migration;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.api.FoldersApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.ExecutionRecord;
import org.openapitools.client.model.ExecutionRecordsMoveData;
import org.openapitools.client.model.Folder;
import org.openapitools.client.model.FolderTransmisionObject;
import org.openapitools.client.model.ProfilePath;

import com.btc.ep.plugins.embeddedplatform.reporting.project.SerializableReportingContainer;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcAddDomainCheckGoals;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcSimulationStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcVectorGenerationStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateCStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateECStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateTLStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcExecutionRecordExportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcInputRestrictionsImportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorImportStep;
import com.btc.ep.plugins.embeddedplatform.util.MigrationSuiteHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.google.gson.Gson;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcMigrationSourceStepExecution extends AbstractBtcStepExecution {

	private static final String EXECUTION_RECORD = "EXECUTION_RECORD";
	private static final long serialVersionUID = 1L;
	private transient BtcMigrationSourceStep step;

	public BtcMigrationSourceStepExecution(BtcMigrationSourceStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	/**
	 * Migration Source Step
	 * <ul>
	 * <li>0. Starts up / connects to EP</li>
	 * <li>1. Loads an existing profile or creates a new one 2. Imports existing
	 * vectors (optional)</li>
	 * <li>3.Creates Domain Check goals (optional)</li>
	 * <li>4. Generates vectors for structural coverage</li>
	 * <li>5. Simulates on the desired execution configs</li>
	 * <li>6. Exports the execution records</li>
	 * <li>7. Saves the profile</li>
	 * </ul>
	 */
	@Override
	protected void performAction() throws Exception {
		Store.globalSuffix = "_source"; // set prefix for profile names
		/*
		 * 0. Startup
		 */
		BtcStartupStep startup = new BtcStartupStep();
		Util.applyMatchingFields(step, startup).start(getContext()).start();

		/*
		 * 1. Load or create the profile
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		FilePath profileFilePath = resolveInAgentWorkspace(profilePath);
		if (!step.isCreateProfilesFromScratch() && profileFilePath.exists()) {
			BtcProfileLoadStep profileLoad = new BtcProfileLoadStep(profilePath);
			Util.applyMatchingFields(step, profileLoad).start(getContext()).start();
		} else if (step.getTlModelPath() != null) {
			BtcProfileCreateTLStep tlProfileCreation = new BtcProfileCreateTLStep(step.getTlModelPath());
			tlProfileCreation.setProfilePath(profilePath);
			Util.applyMatchingFields(step, tlProfileCreation).start(getContext()).start();
		} else if (step.getSlModelPath() != null) {
			BtcProfileCreateECStep ecProfileCreation = new BtcProfileCreateECStep(step.getSlModelPath());
			ecProfileCreation.setProfilePath(profilePath);
			Util.applyMatchingFields(step, ecProfileCreation).start(getContext()).start();
		} else if (step.getCodeModelPath() != null) {
			BtcProfileCreateCStep cProfileCreation = new BtcProfileCreateCStep(step.getCodeModelPath());
			cProfileCreation.setProfilePath(profilePath);
			Util.applyMatchingFields(step, cProfileCreation).start(getContext()).start();
		} else {
			throw new IllegalArgumentException(
					"You must either specify a model, code model or point to an existing profile.");
		}

		/*
		 * 2. Import existing vectors (optional)
		 */
		if (step.getImportDir() != null) {
			BtcVectorImportStep vectorImport = new BtcVectorImportStep(step.getImportDir());
			Util.applyMatchingFields(vectorImport, step).start(getContext()).start();
		}

		/*
		 * 3. Input Restrictions (optional)
		 */
		if (step.getInputRestrictionsPath() != null) {
			new BtcInputRestrictionsImportStep(step.getInputRestrictionsPath()).start(getContext()).start();
		}

		/*
		 * 4. Creates Domain Check Goals (optional)
		 */
		if (step.getDcXmlPath() != null) {
			BtcAddDomainCheckGoals DCG = new BtcAddDomainCheckGoals();
			DCG.setDcXmlPath(step.getDcXmlPath());
			DCG.start(getContext()).start();
		} else if (step.getScopePath() != null) {
			BtcAddDomainCheckGoals DCG = new BtcAddDomainCheckGoals();
			DCG.setScopePath(step.getScopePath());
			DCG.setRaster(step.getRaster());
			DCG.setActivateBoundaryCheck(step.isActivateBoundaryCheck());
			DCG.setActivateRangeViolationCheck(step.isActivateRangeViolationCheck());
			DCG.start(getContext()).start();
		}

		/*
		 * 5. Generate Vectors for structural coverage
		 */
		BtcVectorGenerationStep vectorGeneration = new BtcVectorGenerationStep();
		Util.applyMatchingFields(step, vectorGeneration).start(getContext()).start();

		/*
		 * 6. Simulates on the desired execution configs
		 */
		BtcSimulationStep simulation = new BtcSimulationStep();
		Util.applyMatchingFields(step, simulation).start(getContext()).start();

		/*
		 * 7. Export/Store Execution Records
		 */
		storeExecutionRecords();

		/*
		 * 8. Save profile and wrap up
		 */
		// save the epp to the designated location
		saveProfileAndReportData(profilePath.toString());

		// print success message and return response code
		String msg = "[200] Migration Source successfully executed.";
		log(msg);
		info(msg);
		response = 200;
	}

	/**
	 * Saves the profile, prepares the source-part of the report and dumps it to a
	 * file.
	 *
	 * @throws ApiException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void saveProfileAndReportData(String profilePath) throws Exception {
		// Save Profile
		ProfilesApi profileApi = new ProfilesApi();
		profileApi.saveProfile(new ProfilePath().path(profilePath));

		// Prepare Report Data
		Map<String, String> metadata = profileApi.getCurrentProfile().getMetadata();
		Store.metaInfoSection.setProfileData(metadata);
		Store.reportData.addSection(Store.testStepSection);
		Store.testStepArgumentSection.setSteps(Store.testStepSection.getSteps());
		Store.reportData.addSection(Store.testStepArgumentSection);
		Store.reportData.addSection(Store.pilInfoSection);
		String endDate = Util.DATE_FORMAT.format(new Date());
		Store.reportData.setEndDate(endDate);
		String durationString = Util.getTimeDiffAsString(new Date(), Store.startDate);
		Store.reportData.setDuration(durationString);

		// Prepare container for serialization
		SerializableReportingContainer json = new SerializableReportingContainer();
		json.setStartDate(Store.reportData.getStartDate());
		json.setEndDate(endDate);
		json.setDuration(durationString);
		json.getSections().add(Store.testStepSection);
		json.getSections().add(Store.testStepArgumentSection);
		json.getSections().add(Store.metaInfoSection);

		// dump to file (so it can be reused by the MigrationTarget step
		getContext().get(FilePath.class).child(Store.exportPath + "/reportData.json").write(new Gson().toJson(json),
				StandardCharsets.UTF_8.toString());
		MigrationSuiteHelper.stashFiles("*.epp, *.mdf", getContext());
	}

	/**
	 * Exports the execution records and moves them to a user defined folder in case
	 * the profile is being used directly.
	 *
	 * @throws Exception
	 */
	public void storeExecutionRecords() throws Exception {
		List<String> executionConfigs = Util.getValuesFromCsv(step.getExecutionConfigString());
		// can't initialize the APIs statically (the right API client has not been set
		// at that time)
		ExecutionRecordsApi erApi = new ExecutionRecordsApi();
		FoldersApi folderApi = new FoldersApi();

		List<ExecutionRecord> executionRecords = erApi.getExecutionRecords();
		for (String config : executionConfigs) {
			if (step.isCreateProfilesFromScratch()) {
				// Export Execution Records to be imported in the target profile
				String exportPath = step.getExportPath(); // + executionConfig.replace(" ", "_");
				BtcExecutionRecordExportStep erExportStep = new BtcExecutionRecordExportStep();
				erExportStep.setDir(exportPath);
				erExportStep.setFolderName(config);
				erExportStep.start(getContext()).start();
			} else {
				// Default-case: Move Execution Records to user-defined folder for the
				// regression test
				List<String> erUids = executionRecords.stream()
						.filter(er -> config.equals(er.getExecutionConfig()) && config.equals(er.getFolderName()))
						.map(er -> er.getUid()).collect(Collectors.toList());
				String folderName = MigrationSuiteHelper.getExecutionRecordSourceFolderName(config);
				FolderTransmisionObject folderInfo = new FolderTransmisionObject().folderKind(EXECUTION_RECORD)
						.folderName(folderName);
				Folder folder = folderApi.addFolder(folderInfo);
				ExecutionRecordsMoveData moveData = new ExecutionRecordsMoveData().uiDs(erUids);
				erApi.moveExecutionRecords(folder.getUid(), moveData);
			}
		}
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcMigrationSourceStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	// startup
	private Integer port;
	private Integer timeout;
	private String licensePackage;
	private String installPath;
	private String additionalJvmArgs;

	// profile
	private String profilePath;
	private String startupScriptPath;
	private String matlabVersion;
	private String matlabInstancePolicy; // AUTO, ALWAYS, NEVER
	private String exportPath;
	private Boolean updateRequired;
	private String saveProfileAfterEachStep;
	// this does not mirror fields from other steps -> can be a primitive boolean:
	private boolean createProfilesFromScratch = false;

	// targetlink
	private String tlModelPath;
	private String tlScriptPath;
	private String tlSubsystem;
	private String environmentXmlPath;
	private Boolean reuseExistingCode;
	private String pilConfig;
	private Integer pilTimeout;
	private String calibrationHandling;
	private String testMode;
	private String tlSubsystemFilter;
	private String tlCalibrationFilter;
	private String tlCodeFileFilter;

	// c-code & embeddedcoder
	private String codeModelPath;
	private String compilerShortName;
	private String slModelPath;
	private String slScriptPath;
	private String createWrapperModel;
	private String addModelInfoPath;

	// vector import
	private String importDir;
	private String vectorKind = "TC";
	private String vectorFormat = "TC";

	// tolerance & input-restrictions import
	private String tolerancesXmlPath;
	private String inputRestrictionsPath;
	private String applyTo;
	private String relTolerance;
	private String absToleranceFlp;
	private String absToleranceFxp;
	private Boolean fxpIsMultipleOfLsb;

	// vector generation
	private String pll;
	private String engine;
	private Integer globalTimeout;
	private Integer scopeTimeout;
	private Integer perPropertyTimeout;
	private Boolean considerSubscopes;
	private Boolean analyzeSubscopesHierarchically;
	private Boolean allowDenormalizedFloats;
	private Boolean recheckUnreachable;
	private Integer depthCv;
	private Integer depthAtg;
	private Integer loopUnroll;
	private Boolean robustnessTestFailure;
	private Integer numberOfThreads;
	private String parallelExecutionMode = "BALANCED";

	// domain check goals
	private String scopePath;
	private String dcXmlPath;
	private String raster;
	private boolean activateRangeViolationCheck;
	private boolean activateBoundaryCheck;

	// simulation
	private String executionConfigString = "SIL";
	private String scopesWhitelist;
	private String scopesBlacklist;

	@DataBoundConstructor
	public BtcMigrationSourceStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcMigrationSourceStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcMigrationSource";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Migration Source Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public Integer getPort() {
		return port;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public String getLicensePackage() {
		return licensePackage;
	}

	public String getInstallPath() {
		return installPath;
	}

	public String getAdditionalJvmArgs() {
		return additionalJvmArgs;
	}

	public String getProfilePath() {
		return profilePath;
	}

	public String getStartupScriptPath() {
		return startupScriptPath;
	}

	public String getMatlabVersion() {
		return matlabVersion;
	}

	public String getMatlabInstancePolicy() {
		return matlabInstancePolicy;
	}

	public String getExportPath() {
		return exportPath;
	}

	public Boolean isUpdateRequired() {
		return updateRequired;
	}

	public String getSaveProfileAfterEachStep() {
		return saveProfileAfterEachStep;
	}

	public String getTlModelPath() {
		return tlModelPath;
	}

	public String getTlScriptPath() {
		return tlScriptPath;
	}

	public String getTlSubsystem() {
		return tlSubsystem;
	}

	public String getEnvironmentXmlPath() {
		return environmentXmlPath;
	}

	public Boolean isReuseExistingCode() {
		return reuseExistingCode;
	}

	public String getPilConfig() {
		return pilConfig;
	}

	public Integer getPilTimeout() {
		return pilTimeout;
	}

	public String getCalibrationHandling() {
		return calibrationHandling;
	}

	public String getTestMode() {
		return testMode;
	}

	public String getTlSubsystemFilter() {
		return tlSubsystemFilter;
	}

	public String getTlCalibrationFilter() {
		return tlCalibrationFilter;
	}

	public String getTlCodeFileFilter() {
		return tlCodeFileFilter;
	}

	public String getCodeModelPath() {
		return codeModelPath;
	}

	public String getCompilerShortName() {
		return compilerShortName;
	}

	public String getSlModelPath() {
		return slModelPath;
	}

	public String getSlScriptPath() {
		return slScriptPath;
	}

	public String getCreateWrapperModel() {
		return createWrapperModel;
	}

	public String getAddModelInfoPath() {
		return addModelInfoPath;
	}

	public String getImportDir() {
		return importDir;
	}

	public String getVectorFormat() {
		return vectorFormat;
	}

	public String getVectorKind() {
		return vectorKind;
	}

	public String getTolerancesXmlPath() {
		return tolerancesXmlPath;
	}

	public String getInputRestrictionsPath() {
		return inputRestrictionsPath;
	}

	public String getApplyTo() {
		return applyTo;
	}

	public String getRelTolerance() {
		return relTolerance;
	}

	public String getAbsToleranceFlp() {
		return absToleranceFlp;
	}

	public String getAbsToleranceFxp() {
		return absToleranceFxp;
	}

	public Boolean isFxpIsMultipleOfLsb() {
		return fxpIsMultipleOfLsb;
	}

	public String getExecutionConfigString() {
		return executionConfigString;
	}

	public String getScopesWhitelist() {
		return scopesWhitelist;
	}

	public String getScopesBlacklist() {
		return scopesBlacklist;
	}

	public String getPll() {
		return pll;
	}

	public String getEngine() {
		return engine;
	}

	public Integer getGlobalTimeout() {
		return globalTimeout;
	}

	public Integer getScopeTimeout() {
		return scopeTimeout;
	}

	public Integer getPerPropertyTimeout() {
		return perPropertyTimeout;
	}

	public Boolean isConsiderSubscopes() {
		return considerSubscopes;
	}

	public Boolean isAnalyzeSubscopesHierarchically() {
		return analyzeSubscopesHierarchically;
	}

	public Boolean isAllowDenormalizedFloats() {
		return allowDenormalizedFloats;
	}

	public Boolean isRecheckUnreachable() {
		return recheckUnreachable;
	}

	public Integer getDepthCv() {
		return depthCv;
	}

	public Integer getDepthAtg() {
		return depthAtg;
	}

	public Integer getLoopUnroll() {
		return loopUnroll;
	}

	public Boolean isRobustnessTestFailure() {
		return robustnessTestFailure;
	}

	public Integer getNumberOfThreads() {
		return numberOfThreads;
	}

	public String getParallelExecutionMode() {
		return parallelExecutionMode;
	}

	public String getScopePath() {
		return scopePath;
	}

	public String getDcXmlPath() {
		return dcXmlPath;
	}

	public String getRaster() {
		return raster;
	}

	public boolean isActivateRangeViolationCheck() {
		return activateRangeViolationCheck;
	}

	public boolean isActivateBoundaryCheck() {
		return activateBoundaryCheck;
	}

	public boolean isCreateProfilesFromScratch() {
		return createProfilesFromScratch;

	}

	@DataBoundSetter
	public void setPort(Integer port) {
		this.port = port;
	}

	@DataBoundSetter
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	@DataBoundSetter
	public void setLicensePackage(String licensePackage) {
		this.licensePackage = licensePackage;
	}
	
	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

	@DataBoundSetter
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	@DataBoundSetter
	public void setAdditionalJvmArgs(String additionalJvmArgs) {
		this.additionalJvmArgs = additionalJvmArgs;
	}

	@DataBoundSetter
	public void setStartupScriptPath(String startupScriptPath) {
		this.startupScriptPath = startupScriptPath;
	}

	@DataBoundSetter
	public void setMatlabVersion(String matlabVersion) {
		this.matlabVersion = matlabVersion;
	}

	@DataBoundSetter
	public void setMatlabInstancePolicy(String matlabInstancePolicy) {
		this.matlabInstancePolicy = matlabInstancePolicy;
	}

	@DataBoundSetter
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	@DataBoundSetter
	public void setUpdateRequired(Boolean updateRequired) {
		this.updateRequired = updateRequired;
	}

	@DataBoundSetter
	public void setSaveProfileAfterEachStep(String saveProfileAfterEachStep) {
		this.saveProfileAfterEachStep = saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setTlModelPath(String tlModelPath) {
		this.tlModelPath = tlModelPath;
	}

	@DataBoundSetter
	public void setTlScriptPath(String tlScriptPath) {
		this.tlScriptPath = tlScriptPath;
	}

	@DataBoundSetter
	public void setTlSubsystem(String tlSubsystem) {
		this.tlSubsystem = tlSubsystem;
	}

	@DataBoundSetter
	public void setEnvironmentXmlPath(String environmentXmlPath) {
		this.environmentXmlPath = environmentXmlPath;
	}

	@DataBoundSetter
	public void setReuseExistingCode(Boolean reuseExistingCode) {
		this.reuseExistingCode = reuseExistingCode;
	}

	@DataBoundSetter
	public void setPilConfig(String pilConfig) {
		this.pilConfig = pilConfig;
	}

	@DataBoundSetter
	public void setPilTimeout(Integer pilTimeout) {
		this.pilTimeout = pilTimeout;
	}

	@DataBoundSetter
	public void setCalibrationHandling(String calibrationHandling) {
		this.calibrationHandling = calibrationHandling;
	}

	@DataBoundSetter
	public void setTestMode(String testMode) {
		this.testMode = testMode;
	}

	@DataBoundSetter
	public void setTlSubsystemFilter(String tlSubsystemFilter) {
		this.tlSubsystemFilter = tlSubsystemFilter;
	}

	@DataBoundSetter
	public void setTlCalibrationFilter(String tlCalibrationFilter) {
		this.tlCalibrationFilter = tlCalibrationFilter;
	}

	@DataBoundSetter
	public void setTlCodeFileFilter(String tlCodeFileFilter) {
		this.tlCodeFileFilter = tlCodeFileFilter;
	}

	@DataBoundSetter
	public void setCodeModelPath(String codeModelPath) {
		this.codeModelPath = codeModelPath;
	}

	@DataBoundSetter
	public void setCompilerShortName(String compilerShortName) {
		this.compilerShortName = compilerShortName;
	}

	@DataBoundSetter
	public void setSlModelPath(String slModelPath) {
		this.slModelPath = slModelPath;
	}

	@DataBoundSetter
	public void setSlScriptPath(String slScriptPath) {
		this.slScriptPath = slScriptPath;
	}

	@DataBoundSetter
	public void setCreateWrapperModel(String createWrapperModel) {
		this.createWrapperModel = createWrapperModel;
	}

	@DataBoundSetter
	public void setAddModelInfoPath(String addModelInfoPath) {
		this.addModelInfoPath = addModelInfoPath;
	}

	@DataBoundSetter
	public void setImportDir(String importDir) {
		this.importDir = importDir;
	}

	@DataBoundSetter
	public void setVectorFormat(String vectorFormat) {
		this.vectorFormat = vectorFormat;
	}

	@DataBoundSetter
	public void setVectorKind(String vectorKind) {
		this.vectorKind = vectorKind;
	}

	@DataBoundSetter
	public void setTolerancesXmlPath(String tolerancesXmlPath) {
		this.tolerancesXmlPath = tolerancesXmlPath;
	}

	@DataBoundSetter
	public void setInputRestrictionsPath(String inputRestrictionsPath) {
		this.inputRestrictionsPath = inputRestrictionsPath;
	}

	@DataBoundSetter
	public void setApplyTo(String applyTo) {
		this.applyTo = applyTo;
	}

	@DataBoundSetter
	public void setRelTolerance(String relTolerance) {
		this.relTolerance = relTolerance;
	}

	@DataBoundSetter
	public void setAbsToleranceFlp(String absToleranceFlp) {
		this.absToleranceFlp = absToleranceFlp;
	}

	@DataBoundSetter
	public void setAbsToleranceFxp(String absToleranceFxp) {
		this.absToleranceFxp = absToleranceFxp;
	}

	@DataBoundSetter
	public void setFxpIsMultipleOfLsb(Boolean fxpIsMultipleOfLsb) {
		this.fxpIsMultipleOfLsb = fxpIsMultipleOfLsb;
	}

	@DataBoundSetter
	public void setExecutionConfigString(String executionConfigString) {
		this.executionConfigString = executionConfigString;
	}

	@DataBoundSetter
	public void setScopesWhitelist(String scopesWhitelist) {
		this.scopesWhitelist = scopesWhitelist;
	}

	@DataBoundSetter
	public void setScopesBlacklist(String scopesBlacklist) {
		this.scopesBlacklist = scopesBlacklist;
	}

	@DataBoundSetter
	public void setPll(String pll) {
		this.pll = pll;
	}

	@DataBoundSetter
	public void setEngine(String engine) {
		this.engine = engine;
	}

	@DataBoundSetter
	public void setGlobalTimeout(Integer globalTimeout) {
		this.globalTimeout = globalTimeout;
	}

	@DataBoundSetter
	public void setScopeTimeout(Integer scopeTimeout) {
		this.scopeTimeout = scopeTimeout;
	}

	@DataBoundSetter
	public void setPerPropertyTimeout(Integer perPropertyTimeout) {
		this.perPropertyTimeout = perPropertyTimeout;
	}

	@DataBoundSetter
	public void setConsiderSubscopes(Boolean considerSubscopes) {
		this.considerSubscopes = considerSubscopes;
	}

	@DataBoundSetter
	public void setAnalyzeSubscopesHierarchically(Boolean analyzeSubscopesHierarchically) {
		this.analyzeSubscopesHierarchically = analyzeSubscopesHierarchically;
	}

	@DataBoundSetter
	public void setAllowDenormalizedFloats(Boolean allowDenormalizedFloats) {
		this.allowDenormalizedFloats = allowDenormalizedFloats;
	}

	@DataBoundSetter
	public void setRecheckUnreachable(Boolean recheckUnreachable) {
		this.recheckUnreachable = recheckUnreachable;
	}

	@DataBoundSetter
	public void setDepthCv(Integer depthCv) {
		this.depthCv = depthCv;
	}

	@DataBoundSetter
	public void setDepthAtg(Integer depthAtg) {
		this.depthAtg = depthAtg;
	}

	@DataBoundSetter
	public void setLoopUnroll(Integer loopUnroll) {
		this.loopUnroll = loopUnroll;
	}

	@DataBoundSetter
	public void setRobustnessTestFailure(Boolean robustnessTestFailure) {
		this.robustnessTestFailure = robustnessTestFailure;
	}

	@DataBoundSetter
	public void setNumberOfThreads(Integer numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	@DataBoundSetter
	public void setParallelExecutionMode(String parallelExecutionMode) {
		this.parallelExecutionMode = parallelExecutionMode;
	}

	@DataBoundSetter
	public void setScopePath(String scopePath) {
		this.scopePath = scopePath;
	}

	@DataBoundSetter
	public void setDcXmlPath(String dcXmlPath) {
		this.dcXmlPath = dcXmlPath;
	}

	@DataBoundSetter
	public void setRaster(String raster) {
		this.raster = raster;
	}

	@DataBoundSetter
	public void setActivateRangeViolationCheck(boolean activateRangeViolationCheck) {
		this.activateRangeViolationCheck = activateRangeViolationCheck;
	}

	@DataBoundSetter
	public void setActivateBoundaryCheck(boolean activateBoundaryCheck) {
		this.activateBoundaryCheck = activateBoundaryCheck;
	}

	@DataBoundSetter
	public void setCreateProfilesFromScratch(boolean createProfileFromScratch) {
		this.createProfilesFromScratch = createProfileFromScratch;

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
