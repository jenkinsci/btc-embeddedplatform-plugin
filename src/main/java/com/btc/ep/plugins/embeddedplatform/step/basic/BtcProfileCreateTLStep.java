package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.TLImportInfo;
import org.openapitools.client.model.TLImportInfo.CalibrationHandlingEnum;
import org.openapitools.client.model.TLImportInfo.FixedStepSolverEnum;
import org.openapitools.client.model.TLImportInfo.TestModeEnum;
import org.openapitools.client.model.TLImportInfo.UseExistingCodeEnum;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.step.MatlabAwareStep;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

class BtcProfileCreateTLStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcProfileCreateTLStep step;

	public BtcProfileCreateTLStepExecution(BtcProfileCreateTLStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ProfileCreateTLExecution exec = new ProfileCreateTLExecution(step, logger, getContext());

		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.matlabVersion = Store.matlabVersion;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	
}

/**
 * This class defines what happens when the above step is executed
 */
class ProfileCreateTLExecution extends BtcExecution {

	private static final long serialVersionUID = -2873758361286777998L;
	private BtcProfileCreateTLStep step;
	
	private ArchitecturesApi archApi = new ArchitecturesApi();

	public ProfileCreateTLExecution(BtcProfileCreateTLStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		/*
		 * Preparation
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		String tlModelPath = resolveToString(step.getTlModelPath());
		String tlScriptPath = resolveToString(step.getTlScriptPath());
		preliminaryChecks();
		dataTransferObject.epp = resolveToString(profilePath);
		dataTransferObject.eppName = resolveToPath(profilePath).getFileName().toString();
		dataTransferObject.exportPath = resolveToString(step.getExportPath());
		createEmptyProfile();

		/*
		 * Prepare Matlab
		 */
		prepareMatlab(step);

		/*
		 * Create the profile based on the code model
		 */
		// perform import
		try {
			TLImportInfo info = prepareInfoObject(tlModelPath, tlScriptPath);
			Job job = archApi.importTargetLinkArchitecture(info);
			log("Importing TargetLink architecture...");
			HttpRequester.waitForCompletion(job.getJobID());
		} catch (Exception e) {
			error("Failed to import architecture.", e);
			return response(400);
		}
		/*
		 * Wrapping up, reporting, etc.
		 */
		String msg = "Architecture Import successful.";
		detailWithLink(dataTransferObject.eppName, profilePath);
		log(msg);
		info(msg);
		return response(200);
	}

	/**
	 * Collect everything for the import
	 */
	private TLImportInfo prepareInfoObject(String tlModelPath, String tlScriptPath) throws Exception {
		TLImportInfo info = new TLImportInfo().tlModelFile(tlModelPath).tlInitScript(tlScriptPath)
				.fixedStepSolver(FixedStepSolverEnum.TRUE);
		// Calibration Handling
		CalibrationHandlingEnum calibrationHandling = CalibrationHandlingEnum.EXPLICIT_PARAMETER;
		if (step.getCalibrationHandling().equalsIgnoreCase("LIMITED BLOCKSET")) {
			calibrationHandling = CalibrationHandlingEnum.LIMITED_BLOCKSET;
		} else if (step.getCalibrationHandling().equalsIgnoreCase("OFF")) {
			calibrationHandling = CalibrationHandlingEnum.OFF;
		}
		info.setCalibrationHandling(calibrationHandling);
		// Test Mode (Displays)
		TestModeEnum testMode = TestModeEnum.GREY_BOX;
		if (step.getTestMode().equalsIgnoreCase("BLACK BOX")) {
			testMode = TestModeEnum.BLACK_BOX;
		}
		info.setTestMode(testMode);
		// Legacy Code XML (Environment)
		if (step.getEnvironmentXmlPath() != null) {
			info.setEnvironment(resolveToString(step.getEnvironmentXmlPath()));
		}
		info.setUseExistingCode(step.isReuseExistingCode() ? UseExistingCodeEnum.TRUE : UseExistingCodeEnum.FALSE);
		// TL Subsystem
		if (step.getTlSubsystem() != null) {
			info.setTlSubsystem(step.getTlSubsystem());
		}
		if (step.getSubsystemMatcher() != null) {
			info.setSubsystemMatcher(step.getSubsystemMatcher());
		}
		if (step.getCalibrationMatcher() != null) {
			info.setCalibrationMatcher(step.getCalibrationMatcher());
		}
		if (step.getCodeFileMatcher() != null) {
			info.setCfileMatcher(step.getCodeFileMatcher());
		}
		return info;
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
public class BtcProfileCreateTLStep extends Step implements Serializable, MatlabAwareStep {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath;

	private String tlModelPath;
	private String tlScriptPath;
	private String tlSubsystem;
	private String environmentXmlPath;
	private String pilConfig;
	private String calibrationHandling = "EXPLICIT PARAM";
	private String testMode = "GREY BOX";
	private boolean reuseExistingCode;
	private String subsystemMatcher;
	private String calibrationMatcher;
	private String codeFileMatcher;
	private String startupScriptPath;
	private String matlabVersion;
	private String matlabInstancePolicy = "AUTO";
	private boolean saveProfileAfterEachStep;
	private String licenseLocationString; // mark as deprecated?

	@DataBoundConstructor
	public BtcProfileCreateTLStep(String tlModelPath) {
		super();
		this.tlModelPath = tlModelPath;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcProfileCreateTLStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcProfileCreateTL";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Profile Creation (Targetlink)";
		}
	}

	public String getProfilePath() {
		return profilePath;
	}

	public String getExportPath() {
		return exportPath;
	}

	public String getTlModelPath() {
		return tlModelPath;
	}

	public String getTlScriptPath() {
		return tlScriptPath;
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

	public String getTlSubsystem() {
		return tlSubsystem;
	}

	public String getEnvironmentXmlPath() {
		return environmentXmlPath;
	}

	public String getPilConfig() {
		return pilConfig;
	}

	public String getCalibrationHandling() {
		return calibrationHandling;
	}

	public String getTestMode() {
		return testMode;
	}

	public boolean isReuseExistingCode() {
		return reuseExistingCode;
	}

	public String getSubsystemMatcher() {
		return subsystemMatcher;
	}

	public String getCalibrationMatcher() {
		return calibrationMatcher;
	}

	public String getCodeFileMatcher() {
		return codeFileMatcher;
	}

	@DataBoundSetter
	public void setSubsystemMatcher(String subsystemMatcher) {
		this.subsystemMatcher = subsystemMatcher;
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
	public void setTlSubsystem(String tlSubsystem) {
		this.tlSubsystem = tlSubsystem;
	}

	@DataBoundSetter
	public void setEnvironmentXmlPath(String environmentXmlPath) {
		this.environmentXmlPath = environmentXmlPath;
	}

	@DataBoundSetter
	public void setPilConfig(String pilConfig) {
		this.pilConfig = pilConfig;
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
	public void setReuseExistingCode(boolean reuseExistingCode) {
		this.reuseExistingCode = reuseExistingCode;
	}

	@DataBoundSetter
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	@DataBoundSetter
	public void setTlScriptPath(String tlScriptPath) {
		this.tlScriptPath = tlScriptPath;
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
	public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
		this.saveProfileAfterEachStep = saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;
	}

	@DataBoundSetter
	public void setMatlabVersion(String matlabVersion) {
		this.matlabVersion = matlabVersion;
	}

	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class