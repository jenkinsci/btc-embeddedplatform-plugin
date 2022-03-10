package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.ECImportInfo;
import org.openapitools.client.model.ECImportInfo.ParameterHandlingEnum;
import org.openapitools.client.model.ECImportInfo.TestModeEnum;
import org.openapitools.client.model.ECWrapperImportInfo;
import org.openapitools.client.model.ECWrapperResultInfo;
import org.openapitools.client.model.Job;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileCreateECStepExecution extends AbstractBtcStepExecution {

	private static final long serialVersionUID = 1L;
	private BtcProfileCreateECStep step;
	private ProfilesApi profilesApi = new ProfilesApi();
	private ArchitecturesApi archApi = new ArchitecturesApi();

	public BtcProfileCreateECStepExecution(BtcProfileCreateECStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	/*
	 * Put the desired action here: - checking preconditions - access step
	 * parameters (field step: step.getFoo()) - calling EP Rest API - print text to
	 * the Jenkins console (field: jenkinsConsole) - set resonse code (field:
	 * response)
	 */
	@Override
	protected void performAction() throws Exception {
		/*
		 * Preparation
		 */
		String slModelPath = toRemoteAbsolutePathString(step.getSlModelPath());
		String slScriptPath = toRemoteAbsolutePathString(step.getSlScriptPath());
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		preliminaryChecks();
		Store.epp = resolveInAgentWorkspace(profilePath);
		Store.exportPath = toRemoteAbsolutePathString(step.getExportPath() != null ? step.getExportPath() : "reports");
		profilesApi.createProfile(true);

		/*
		 * Prepare Matlab
		 */
		log("Preparing Matlab...");
		Util.configureMatlabConnection(step.getMatlabVersion(), step.getMatlabInstancePolicy());
		// TODO: Execute Startup Script (requires EP-2535)

		// TODO: Create Wrapper Model (requires EP-2538)
		String modelPath = slModelPath.toString();
		String scriptPath = slScriptPath == null ? null : slScriptPath.toString();
		if (step.isCreateWrapperModel()) {
			ECWrapperImportInfo wrapperInfo = new ECWrapperImportInfo();
			wrapperInfo.setEcModelFile(modelPath);
			wrapperInfo.setEcInitScript(scriptPath);
			try {
				Job job = archApi.createEmbeddedCoderCWrapperModel(wrapperInfo);
				log("Creating wrapper model for autosar component...");
				Object resultObj = HttpRequester.waitForCompletion(job.getJobID(), "result");
				modelPath = (String) ((Map<?, ?>) resultObj).get(ECWrapperResultInfo.SERIALIZED_NAME_EC_MODEL_FILE);
				scriptPath = (String) ((Map<?, ?>) resultObj).get(ECWrapperResultInfo.SERIALIZED_NAME_EC_INIT_FILE);
				log("EmbeddedCoder Autosar wrapper model creation succeeded.");
			} catch (Exception e) {
				log("ERROR. Failed to create wrapper model: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
		}

		/*
		 * Create the profile based on the code model
		 */
		ECImportInfo info = new ECImportInfo().ecModelFile(modelPath).ecInitScript(scriptPath).fixedStepSolver(true);
		if (step.getSubsystemMatcher() != null) {
			info.setSubsystemMatcher(step.getSubsystemMatcher());
		}
		if (step.getCalibrationMatcher() != null) {
			info.setParameterMatcher(step.getCalibrationMatcher());
		}
		if (step.getCodeFileMatcher() != null) {
			// TODO: Implement as soon as the EC arch import offers a code file matcher
			// info.setCodeFileMatcher(step.getCodeFileMatcher());
		}
		configureParameterHandling(info, step.getParameterHandling());
		configureTestMode(info, step.getTestMode());
		try {
			Job job = archApi.importEmbeddedCoderArchitecture(info);
			log("Importing EmbeddedCoder architecture '" + new File(modelPath).getName() + "'...");
			HttpRequester.waitForCompletion(job.getJobID());
		} catch (Exception e) {
			log("ERROR: Failed to import architecture: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
		}
		/*
		 * Wrapping up, reporting, etc.
		 */
		String msg = "Architecture Import successful.";
		detailWithLink(Store.epp.getName(), profilePath.toString());
		response = 200;
		log(msg);
		info(msg);
	}

	/**
	 * Derives the enumValue from the given string and sets it on the ECImportInfo
	 * object.
	 *
	 * @param info              the ECImportInfo object
	 * @param parameterHandling the string from the user to pick the right enum
	 * @throws ApiException in case of invalid enum string
	 */
	private void configureParameterHandling(ECImportInfo info, String parameterHandling) throws ApiException {
		try {
			ParameterHandlingEnum valueAsEnum = ParameterHandlingEnum.fromValue(parameterHandling.toUpperCase());
			info.setParameterHandling(valueAsEnum);
		} catch (Exception e) {
			throw new ApiException("The specifide parameterHandling enum is not valid. Possible values are: "
					+ ParameterHandlingEnum.values());
		}
	}

	/**
	 * Derives the enumValue from the given string and sets it on the ECImportInfo
	 * object.
	 *
	 * @param info     the ECImportInfo object
	 * @param testMode the string from the user to pick the right enum
	 * @throws ApiException in case of invalid enum string
	 */
	private void configureTestMode(ECImportInfo info, String testMode) throws ApiException {
		try {
			TestModeEnum valueAsEnum = TestModeEnum.fromValue(testMode.toUpperCase());
			info.setTestMode(valueAsEnum);
		} catch (Exception e) {
			throw new ApiException(
					"The specifide testMode enum is not valid. Possible values are: " + TestModeEnum.values());
		}
	}

	/**
	 * Discards any loaded profile and warns in case the obsolete option
	 * "licenseLocationString" is used.
	 *
	 * @param profilePath      the profile path
	 * @param slModelPath      the slModelPath
	 * @param addInfoModelPath
	 */
	private void preliminaryChecks() {
		if (step.getLicenseLocationString() != null) {
			log("the option 'licenseLocationString' of the btcProfileCreate / btcProfileLoad steps has no effect and will be ignored. Please specify this option with the btcStartup step.");
		}
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcProfileCreateECStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath;

	private String slModelPath;
	private String slScriptPath;
	private String parameterHandling = ParameterHandlingEnum.EXPLICIT_PARAMETER.toString();
	private String testMode = TestModeEnum.GREY_BOX.toString();
	private String startupScriptPath;
	private String matlabVersion;
	private String matlabInstancePolicy = "AUTO";

	private String subsystemMatcher;
	private String calibrationMatcher;
	private String codeFileMatcher;
	private boolean saveProfileAfterEachStep;
	private boolean createWrapperModel;
	private String licenseLocationString; // mark as deprecated?

	@DataBoundConstructor
	public BtcProfileCreateECStep(String slModelPath) {
		super();
		this.slModelPath = slModelPath;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcProfileCreateECStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		/*
		 * This specifies the step name that the the user can use in his Jenkins
		 * Pipeline - for example: btcStartup installPath: 'C:/Program
		 * Files/BTC/ep2.9p0', port: 29267
		 */
		@Override
		public String getFunctionName() {
			return "btcProfileCreateEC";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Profile Creation (EmbeddedCoder)";
		}
	}

	public String getSubsystemMatcher() {
		return subsystemMatcher;
	}

	public void setSubsystemMatcher(String subsystemMatcher) {
		this.subsystemMatcher = subsystemMatcher;
	}

	public String getCalibrationMatcher() {
		return calibrationMatcher;
	}

	public String getProfilePath() {
		return profilePath;
	}

	public String getExportPath() {
		return exportPath;
	}

	public String getSlModelPath() {
		return slModelPath;
	}

	public String getSlScriptPath() {
		return slScriptPath;
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

	public boolean isSaveProfileAfterEachStep() {
		return saveProfileAfterEachStep;
	}

	public String getLicenseLocationString() {
		return licenseLocationString;
	}

	public String getParameterHandling() {
		return parameterHandling;

	}

	public String getTestMode() {
		return testMode;

	}

	public boolean isCreateWrapperModel() {
		return createWrapperModel;
	}

	public String getCodeFileMatcher() {
		return codeFileMatcher;
	}

	@DataBoundSetter
	public void setCalibrationMatcher(String calibrationMatcher) {
		this.calibrationMatcher = calibrationMatcher;
	}

	@DataBoundSetter
	public void setCodeFileMatcher(String codeFileMatcher) {
		this.codeFileMatcher = codeFileMatcher;
	}

	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

	@DataBoundSetter
	public void setTestMode(String testMode) {
		this.testMode = testMode;

	}

	@DataBoundSetter
	public void setParameterHandling(String parameterHandling) {
		this.parameterHandling = parameterHandling;

	}

	@DataBoundSetter
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	@DataBoundSetter
	public void setSlScriptPath(String slScriptPath) {
		this.slScriptPath = slScriptPath;
	}

	@DataBoundSetter
	public void setStartupScriptPath(String startupScriptPath) {
		this.startupScriptPath = startupScriptPath;
	}

	@DataBoundSetter
	public void setMatlabInstancePolicy(String matlabInstancePolicy) {
		this.matlabInstancePolicy = matlabInstancePolicy;
	}

	@DataBoundSetter
	public void setMatlabVersion(String matlabVersion) {
		this.matlabVersion = matlabVersion;
	}

	@DataBoundSetter
	public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
		this.saveProfileAfterEachStep = saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;
	}

	@DataBoundSetter
	public void setCreateWrapperModel(boolean createWrapperModel) {
		this.createWrapperModel = createWrapperModel;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
