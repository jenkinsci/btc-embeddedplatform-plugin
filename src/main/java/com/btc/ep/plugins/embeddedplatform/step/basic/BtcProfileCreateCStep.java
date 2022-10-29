package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

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
import org.openapitools.client.model.CCodeImportInfo;
import org.openapitools.client.model.Job;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.step.MatlabAwareStep;
import com.btc.ep.plugins.embeddedplatform.util.CompilerHelper;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

class BtcProfileCreateCStepExecution extends SynchronousStepExecution<Object> {
	
	private static final long serialVersionUID = 1L;
	private BtcProfileCreateCStep step;

	public BtcProfileCreateCStepExecution(BtcProfileCreateCStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ProfileCreateCExecution exec = new ProfileCreateCExecution(step, logger, getContext());
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.matlabVersion = Store.matlabVersion;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class ProfileCreateCExecution extends BtcExecution {
	
	private static final long serialVersionUID = 5227884736793053138L;
	private BtcProfileCreateCStep step;
	private transient ArchitecturesApi archApi;

	public ProfileCreateCExecution(BtcProfileCreateCStep step, PrintStream logger, StepContext context) {
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
		String codeModelPath = resolveToString(step.getCodeModelPath());
		preliminaryChecks(codeModelPath);
		dataTransferObject.epp = resolveToString(profilePath);
		dataTransferObject.eppName = resolveToPath(profilePath).getFileName().toString();
		dataTransferObject.exportPath = resolveToString(step.getExportPath());
		createEmptyProfile();

		// Matlab stuff
		prepareMatlab(step);

		/*
		 * Create the profile based on the code model
		 */
		CompilerHelper.setCompilerWithFallback(step.getCompilerShortName(), HttpRequester.printStream);
		CCodeImportInfo info = new CCodeImportInfo().modelFile(codeModelPath.toString());
		Job job = null;
		String msg;
		try {
			
			// DEBUG OUTPUT
			// DEBUG OUTPUT
			log("Code Model path: " + codeModelPath);
			// DEBUG OUTPUT
			// DEBUG OUTPUT
			
			job = archApi.importCCodeArchitecture(info);
			log("Importing C-Code architecture...");
			HttpRequester.waitForCompletion(job.getJobID(), "result");
			/*
			 * Wrapping up, reporting, etc.
			 */
			msg = "Architecture Import successful.";
			detailWithLink(dataTransferObject.eppName, profilePath);
			info(msg);
			log(msg);
		} catch (Exception e) {
			error("Failed to import C-Code architecture.", e);
			return response(400);
		}
		return getResponse();
	}

	/**
	 * Checks if the profilePath and codeModelPath are valid (!= null), discards any
	 * loaded profile and warns in case the obsolete option "licenseLocationString"
	 * is used.
	 *
	 * @param codeModelPath the code model path
	 */
	private void preliminaryChecks(String codeModelPath) {
		if (step.getLicenseLocationString() != null) {
			log("the option 'licenseLocationString' of the btcProfileCreate / btcProfileLoad steps has no effect and will be ignored. Please specify this option with the btcStartup step.");
		}
		checkArgument(codeModelPath != null, "No valid code model path was provided: " + step.getCodeModelPath());
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcProfileCreateCStep extends Step implements Serializable, MatlabAwareStep {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath = "reports";

	private String codeModelPath;
	private String startupScriptPath;
	private String compilerShortName;
	private String matlabVersion;
	private String matlabInstancePolicy = "AUTO";
	private boolean saveProfileAfterEachStep;
	private String licenseLocationString; // mark as deprecated?

	@DataBoundConstructor
	public BtcProfileCreateCStep(String codeModelPath) {
		super();
		this.codeModelPath = codeModelPath;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcProfileCreateCStepExecution(this, context);
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
			return "btcProfileCreateC";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Profile Creation (C-Code)";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getProfilePath() {
		return profilePath;

	}

	public String getExportPath() {
		return exportPath;

	}

	public String getCodeModelPath() {
		return codeModelPath;
	}

	public String getStartupScriptPath() {
		return startupScriptPath;
	}

	public String getLicenseLocationString() {
		return licenseLocationString;
	}

	public String getMatlabVersion() {
		return matlabVersion;
	}

	public String getMatlabInstancePolicy() {
		return matlabInstancePolicy;
	}

	public String getCompilerShortName() {
		return compilerShortName;
	}

	public boolean isSaveProfileAfterEachStep() {
		return saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setCompilerShortName(String compilerShortName) {
		this.compilerShortName = compilerShortName;
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
	public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
		this.saveProfileAfterEachStep = saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;
	}

	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

	@DataBoundSetter
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
