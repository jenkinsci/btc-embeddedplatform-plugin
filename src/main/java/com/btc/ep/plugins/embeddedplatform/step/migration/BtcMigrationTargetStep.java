package com.btc.ep.plugins.embeddedplatform.step.migration;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
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
import org.openapitools.client.api.RegressionTestsApi;
import org.openapitools.client.model.ExecutionRecord;
import org.openapitools.client.model.ExecutionRecordsMoveData;
import org.openapitools.client.model.Folder;
import org.openapitools.client.model.FolderTransmisionObject;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RegressionTest;
import org.openapitools.client.model.RegressionTest.VerdictStatusEnum;
import org.openapitools.client.model.RegressionTestExecutionData;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReportSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.MetaInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.PilInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.SerializableReportingContainer;
import com.btc.ep.plugins.embeddedplatform.reporting.project.StepArgSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateCStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateECStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateTLStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcExecutionRecordExportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcExecutionRecordImportStep;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.google.gson.Gson;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcMigrationTargetStepExecution extends AbstractBtcStepExecution {

	private static final String EXECUTION_RECORD = "EXECUTION_RECORD";
	private static final long serialVersionUID = 1L;
	private BtcMigrationTargetStep step;

	public BtcMigrationTargetStepExecution(BtcMigrationTargetStep step, StepContext context) {
		super(step, context);
		this.step = step;

	}

	/*
	 * Migration Target Step 0. Starts up / connects to EP 1. Loads an existing
	 * profile or creates a new one 2. Import Execution Records (if new profile) 3.
	 * Tolerances 4. Runs the regression test 5. Saves the profile
	 */
	@Override
	protected void performAction() throws Exception {
		try {
			initializeReporting();
		} catch (Exception e) {
			log("WARNING reporting may not work for this step: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			warning();
		}

		// TODO: Check interfaces on relevant scopes
		// ------> detect if outputs are removed
		// ------> (1) before/after arch update or
		// ------> (2) if profile created from scratch: messages during ER import

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
			BtcProfileLoadStep profileLoad = new BtcProfileLoadStep(profilePath.toString());
			Util.applyMatchingFields(step, profileLoad).start(getContext()).start();
		} else if (step.getTlModelPath() != null) {
			BtcProfileCreateTLStep tlProfileCreation = new BtcProfileCreateTLStep(step.getTlModelPath());
			Util.applyMatchingFields(step, tlProfileCreation).start(getContext()).start();
		} else if (step.getSlModelPath() != null) {
			BtcProfileCreateECStep ecProfileCreation = new BtcProfileCreateECStep(step.getSlModelPath());
			Util.applyMatchingFields(step, ecProfileCreation).start(getContext()).start();
		} else if (step.getCodeModelPath() != null) {
			BtcProfileCreateCStep cProfileCreation = new BtcProfileCreateCStep(step.getCodeModelPath());
			Util.applyMatchingFields(step, cProfileCreation).start(getContext()).start();
		} else {
			throw new IllegalArgumentException(
					"You must either specify a model, code model or point to an existing profile.");
		}

		/*
		 * 2. Import Execution Records
		 */
		List<String> executionConfigs = Util.getValuesFromCsv(step.getExecutionConfigString());
		FoldersApi folderApi = new FoldersApi();
		if (step.getImportDir() != null) {
			for (String config : executionConfigs) {
				FolderTransmisionObject folderKind = new FolderTransmisionObject().folderKind(EXECUTION_RECORD);
				try {
					Folder folder = folderApi.addFolder(folderKind);
					BtcExecutionRecordImportStep importStep = new BtcExecutionRecordImportStep(step.getImportDir());
					importStep.setFolderName(folder.getName());
					importStep.start(getContext()).start();
				} catch (Exception e) {
					log("ERROR migrating on object '" + config + "': '" + e.getMessage());
					try {
						log(((ApiException) e).getResponseBody());
					} catch (Exception idc) {
					}
					;
					error();
				}
			}

		}

		/*
		 * 3. Tolerances
		 */
//        if (step.getTolerancesXmlPath() != null) {
//            BtcToleranceImportStep toleranceImport = new BtcToleranceImportStep(step.getTolerancesXmlPath());
//            toleranceImport.setUseCase("B2B");
//            toleranceImport.start(getContext()).start();
//        }

		for (String config : executionConfigs) {
			RegressionTestsApi regressionTestApi = new RegressionTestsApi();
			List<Folder> erFolders = null;
			try {
				erFolders = folderApi.getFoldersByQuery(null, EXECUTION_RECORD).stream().filter(f -> !f.getIsDefault())
						.collect(Collectors.toList());
			} catch (Exception e) {
				log("ERROR querying folderApi: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
			checkArgument(!erFolders.isEmpty(), "No user-defined Execution Record Folders available.");
			RegressionTestExecutionData data = new RegressionTestExecutionData();
			FolderTransmisionObject folderKind = new FolderTransmisionObject().folderKind(EXECUTION_RECORD);
			Folder targetFolder = null;
			try {
				targetFolder = folderApi.addFolder(folderKind);
			} catch (Exception e) {
				log("ERROR adding folder " + folderKind.getFolderName() + e.getMessage());
				log("folder will not be processed!");
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
			data.setCompFolderUID(targetFolder.getUid());
			data.setCompMode(config);
			// TODO: Need naming rule for folders incl. the execution config (EP-2578)
			Job job = null;
			try {
				job = regressionTestApi.executeRegressionTestOnFolder(erFolders.get(0).getUid(), data);
			} catch (Exception e) {
				log("ERROR executing regression test: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
			Map<?, ?> resultMap = (Map<?, ?>) HttpRequester.waitForCompletion(job.getJobID(), "result");
			String regressionTestId = (String) resultMap.get("uid");
			RegressionTest regressionTest = null;
			try {
				regressionTest = regressionTestApi.getTestByUID1(regressionTestId);
			} catch (Exception e) {
				log("ERROR querying folderApi: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
			// status, etc.
			String info = regressionTest.getComparisons().size() + " comparison(s), " + regressionTest.getPassed()
					+ " passed, " + regressionTest.getFailed() + " failed, " + regressionTest.getError() + " error(s)";
			info(info);
			String verdictStatus = regressionTest.getVerdictStatus().toString();
			if (VerdictStatusEnum.PASSED.name().equalsIgnoreCase(verdictStatus)) {
				status(Status.OK).passed().result(verdictStatus);
				response = 200;
			} else if (VerdictStatusEnum.FAILED_ACCEPTED.name().equalsIgnoreCase(verdictStatus)) {
				status(Status.OK).passed().result(verdictStatus);
				response = 201;
			} else if (VerdictStatusEnum.FAILED.name().equalsIgnoreCase(verdictStatus)) {
				status(Status.OK).failed().result(verdictStatus);
				response = 300;
			} else if (VerdictStatusEnum.ERROR.name().equalsIgnoreCase(verdictStatus)) {
				status(Status.ERROR).result(verdictStatus);
				response = 400;
			} else {
				status(Status.ERROR).result(verdictStatus);
				response = 500;
			}
		}

		/*
		 * 5. Save profile and wrap up
		 */
		BtcWrapUpStep wrapUp = new BtcWrapUpStep();
		Util.applyMatchingFields(step, wrapUp);
		wrapUp.setCloseEp(false);
		wrapUp.start(getContext()).start();

		// print success message and return response code
		log("--> [200] Migration Target successfully executed.");
		response = 200;
	}

	/**
	 * Loads the source-part of the migration report so it can be continued. *
	 */
	private void initializeReporting() {
		Store.globalSuffix = "_target"; // set prefix for profile names
		String reportDataJson = Util.readStringFromFile(Store.exportPath + "/reportData.json");
		SerializableReportingContainer reportingContainer = new Gson().fromJson(reportDataJson,
				SerializableReportingContainer.class);
		Store.reportData = new JenkinsAutomationReport();
		Store.reportData.setStartDate(reportingContainer.getStartDate());
		for (JenkinsAutomationReportSection section : reportingContainer.getSections()) {
			if (section.getSectionName().equals("Test Automation Steps")) {
				Store.testStepSection = new TestStepSection(section);
			} else if (section.getSectionName().equals("Test Automation Arguments")) {
				Store.testStepArgumentSection = new StepArgSection(section);
			} else if (section.getSectionName().equals("PIL Runtime Information")) {
				Store.pilInfoSection = new PilInfoSection(section);
			} else if (section.getSectionName().equals("Meta Information")) {
				Store.metaInfoSection = new MetaInfoSection(section);
			}
		}
		try {
			Date sourceStartDate = Util.DATE_FORMAT.parse(reportingContainer.getStartDate());
			Date sourceEndDate = Util.DATE_FORMAT.parse(reportingContainer.getEndDate());
			long sourceDurationMs = sourceEndDate.getTime() - sourceStartDate.getTime();
			Store.startDate = new Date(System.currentTimeMillis() - sourceDurationMs);
		} catch (Exception e) {
			e.printStackTrace();
			Store.startDate = new Date();
		}

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

		List<ExecutionRecord> executionRecords = erApi.getExecutionRecords2();
		// TODO: query all Execution configs if nothing is specified (requires EP-2536)
		for (String config : executionConfigs) {
			if (step.isCreateProfilesFromScratch()) {
				// Export Execution Records to be imported in the target profile
				String exportPath = step.getExportPath(); // + executionConfig.replace(" ", "_");
				BtcExecutionRecordExportStep erExportStep = new BtcExecutionRecordExportStep();
				erExportStep.setFolderName(exportPath);
				erExportStep.start(getContext()).start();
			} else {
				// Move Execution Records to user-defined folder for the regression test
				List<String> erUids = executionRecords.stream()
						.filter(er -> config.equals(er.getExecutionConfig()) && config.equals(er.getFolderName()))
						.map(er -> er.getUid()).collect(Collectors.toList());
				FolderTransmisionObject folderKind = new FolderTransmisionObject().folderKind(EXECUTION_RECORD);
				Folder folder = folderApi.addFolder(folderKind);
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
public class BtcMigrationTargetStep extends Step implements Serializable {

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
	private Boolean updateRequired = true;
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

	// tolerance & input-restrictions import
	private String tolerancesXmlPath;
	private String applyTo;
	private String relTolerance;
	private String absToleranceFlp;
	private String absToleranceFxp;
	private Boolean fxpIsMultipleOfLsb;

	// simulation
	private String importDir;
	private String executionConfigString = "SIL";
	private String scopesWhitelist;
	private String scopesBlacklist;

	@DataBoundConstructor
	public BtcMigrationTargetStep(String profilePath) {
		super();
		this.profilePath = profilePath;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcMigrationTargetStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcMigrationTarget";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Migration Target Step";
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

	public String getTolerancesXmlPath() {
		return tolerancesXmlPath;
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

	public boolean isCreateProfilesFromScratch() {
		return createProfilesFromScratch;

	}

	public String getImportDir() {
		return importDir;

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
	public void setTolerancesXmlPath(String tolerancesXmlPath) {
		this.tolerancesXmlPath = tolerancesXmlPath;
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
	public void setCreateProfilesFromScratch(boolean createProfileFromScratch) {
		this.createProfilesFromScratch = createProfileFromScratch;

	}

	@DataBoundSetter
	public void setImportDir(String importDir) {
		this.importDir = importDir;

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
