package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.SLImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.step.MatlabAwareStep;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

class BtcProfileCreateSLStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcProfileCreateSLStep step;

	public BtcProfileCreateSLStepExecution(BtcProfileCreateSLStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ProfileCreateSLExecution exec = new ProfileCreateSLExecution(step, logger, getContext());

		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.matlabVersion = Store.matlabVersion;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	
}

class ProfileCreateSLExecution extends BtcExecution {
	
	private static final long serialVersionUID = 4219311637704025602L;
	private BtcProfileCreateSLStep step;
	private transient ArchitecturesApi archApi;
	
	public ProfileCreateSLExecution(BtcProfileCreateSLStep step, PrintStream logger, StepContext context) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		archApi = new ArchitecturesApi();
		/*
		 * Preparation
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		String slModelPath = resolveToString(step.getSlModelPath());
		String slScriptPath = resolveToString(step.getSlScriptPath());
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
		 * SL Architecture Import
		 */
		try {
			SLImportInfo info = new SLImportInfo().slModelFile(slModelPath.toString())
					.slInitScriptFile(slScriptPath.toString());
			Job job = archApi.importSimulinkArchitecture(info);
			log("Importing Simulink architecture...");
			HttpRequester.waitForCompletion(job.getJobID());
		} catch (Exception e) {
			error("Failed to import architecture.", e);
			return response(400);
		}
		/*
		 * Wrapping up, reporting, etc.
		 */
		String msg = "Architecture Import successful.";
		detailWithLink(dataTransferObject.eppName, profilePath.toString());
		log(msg);
		info(msg);
		return response(200);
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
public class BtcProfileCreateSLStep extends Step implements Serializable, MatlabAwareStep {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath = "reports";

	private String slModelPath;
	private String slScriptPath;
	private String startupScriptPath;
	private String matlabVersion;
	private String matlabInstancePolicy = "AUTO";
	private boolean saveProfileAfterEachStep;
	
	@Deprecated
	private String licenseLocationString; // mark as deprecated?

	@DataBoundConstructor
	public BtcProfileCreateSLStep(String slModelPath) {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcProfileCreateSLStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcProfileCreateSL";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Profile Creation (Simulink)";
		}
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

	@Deprecated
	public String getLicenseLocationString() {
		return licenseLocationString;
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
	@Deprecated
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;
	}

	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
