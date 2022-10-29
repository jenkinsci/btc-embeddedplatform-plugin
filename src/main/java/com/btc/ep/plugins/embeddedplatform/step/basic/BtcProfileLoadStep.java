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
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.ProfilePath;
import org.openapitools.client.model.UpdateModelPath;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.step.MatlabAwareStep;
import com.btc.ep.plugins.embeddedplatform.util.CompilerHelper;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileLoadStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcProfileLoadStep step;

	public BtcProfileLoadStepExecution(BtcProfileLoadStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ProfileLoadExecution exec = new ProfileLoadExecution(step, logger, getContext());

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
class ProfileLoadExecution extends BtcExecution {
	
	private static final long serialVersionUID = -3030236847736474862L;
	private BtcProfileLoadStep step;
	private transient ArchitecturesApi archApi;

	public ProfileLoadExecution(BtcProfileLoadStep step, PrintStream logger, StepContext context) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}


	
	@Override
	protected Object performAction() throws Exception {
		archApi = new ArchitecturesApi();
		/*
		 * Preliminary checks
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		dataTransferObject.epp = resolveToString(profilePath);
		dataTransferObject.eppName = resolveToPath(profilePath).getFileName().toString();
		dataTransferObject.exportPath = resolveToString(step.getExportPath());

		/*
		 * Load the profile
		 */
		log("Loading profile '" + dataTransferObject.eppName + "'");
		String msg = null;
		try {
			openProfile(dataTransferObject.epp.toString());
			updateModelPaths();
			msg = "Successfully loaded the profile";
			response(200);
		} catch (Exception e) {
			error("Problem while opening the profile.", e);
			return response(400);
		}

		/*
		 * Prepare Matlab if needed
		 */
		try {
			boolean hasModelBasedArchitecture = archApi.getArchitectures(null).stream()
					.filter(arch -> "Simulink".equals(arch.getArchitectureKind())
							|| "TargetLink".equals(arch.getArchitectureKind()))
					.findAny().isPresent();
			if (hasModelBasedArchitecture || step.getStartupScriptPath() != null) {
				// Prepare Matlab
				prepareMatlab(step);
			}
		} catch (Exception e) {
			error("Failed to prepare Matlab.", e);
			return response (400);
		}

		/*
		 * Compiler Settings
		 */
		String compilerShortName = step.getCompilerShortName();
		if (compilerShortName != null) {
			CompilerHelper.setCompiler(compilerShortName);
		}
		/*
		 * Update architecture if required
		 */
		if (step.isUpdateRequired()) {
			try {
				// FIXME: workaround for EP-2752
				new ProfilesApi().saveProfile(new ProfilePath().path(dataTransferObject.epp.toString()));
				// ... end of workaround
				Job archUpdate = archApi.architectureUpdate();
				log("Updating architecture...");
				HttpRequester.waitForCompletion(archUpdate.getJobID());
				msg += " (incl. arch-update)";
				response(201);
			} catch (Exception e) {
				error("Failed to update architecture.", e);
				return response(400);
			}
		}
		log(msg + ".");
		detailWithLink(dataTransferObject.eppName, "../artifact/" + dataTransferObject.eppName);
		info(msg + ".");
		return getResponse();
	}

	/**
	 * Checks if the user passed any model paths. If that's the case this method
	 * updates them in the profile.
	 * 
	 * @throws Exception
	 */
	private void updateModelPaths() throws Exception {
		UpdateModelPath updateModelPath = new UpdateModelPath();
		// resolve paths from pipeline
		String path;
		path = resolveToString(step.getTlModelPath());
		if (path != null) {
			updateModelPath.setTlModelFile(path);
		}
		path = resolveToString(step.getTlScriptPath());
		if (path != null) {
			updateModelPath.setTlInitScript(path);
		}
		path = resolveToString(step.getSlModelPath());
		if (path != null) {
			updateModelPath.setSlModelFile(path);
		}
		path = resolveToString(step.getSlScriptPath());
		if (path != null) {
			updateModelPath.setSlInitScript(path);
		}
		path = resolveToString(step.getAddInfoModelPath());
		if (path != null) {
			updateModelPath.setAddModelInfo(path);
		}
		path = resolveToString(step.getCodeModelPath());
		if (path != null) {
			updateModelPath.setEnvironment(path);
		}
		archApi.updateModelPaths("", updateModelPath);
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcProfileLoadStep extends Step implements Serializable, MatlabAwareStep {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath = "reports";
	private boolean updateRequired;

	private String tlModelPath;
	private String tlScriptPath;
	private String environmentXmlPath;
	private String slModelPath;
	private String slScriptPath;
	private String addInfoModelPath;
	private String codeModelPath;
	private String startupScriptPath;
	private String compilerShortName;
	private String pilConfig;
	private int pilTimeout;
	private String matlabVersion;
	private String matlabInstancePolicy = "AUTO";
	private boolean saveProfileAfterEachStep;
	private String licenseLocationString; // mark as deprecated?

	@DataBoundConstructor
	public BtcProfileLoadStep(String profilePath) {
		super();
		this.profilePath = profilePath;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcProfileLoadStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcProfileLoad";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Profile Load Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getProfilePath() {
		return profilePath;

	}

	public boolean isUpdateRequired() {
		return updateRequired;

	}

	@DataBoundSetter
	public void setUpdateRequired(boolean updateRequired) {
		this.updateRequired = updateRequired;
	}

	public String getExportPath() {
		return exportPath;

	}

	@DataBoundSetter
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;

	}

	public String getTlModelPath() {
		return tlModelPath;
	}

	@DataBoundSetter
	public void setTlModelPath(String tlModelPath) {
		this.tlModelPath = tlModelPath;
	}

	public String getTlScriptPath() {
		return tlScriptPath;
	}

	@DataBoundSetter
	public void setTlScriptPath(String tlScriptPath) {
		this.tlScriptPath = tlScriptPath;
	}

	public String getEnvironmentXmlPath() {
		return environmentXmlPath;
	}

	@DataBoundSetter
	public void setEnvironmentXmlPath(String environmentXmlPath) {
		this.environmentXmlPath = environmentXmlPath;
	}

	public String getSlModelPath() {
		return slModelPath;
	}

	@DataBoundSetter
	public void setSlModelPath(String slModelPath) {
		this.slModelPath = slModelPath;
	}

	public String getSlScriptPath() {
		return slScriptPath;
	}

	@DataBoundSetter
	public void setSlScriptPath(String slScriptPath) {
		this.slScriptPath = slScriptPath;
	}

	public String getAddInfoModelPath() {
		return addInfoModelPath;
	}

	@DataBoundSetter
	public void setAddInfoModelPath(String addInfoModelPath) {
		this.addInfoModelPath = addInfoModelPath;
	}

	public String getCodeModelPath() {
		return codeModelPath;
	}

	@DataBoundSetter
	public void setCodeModelPath(String codeModelPath) {
		this.codeModelPath = codeModelPath;
	}

	public String getStartupScriptPath() {
		return startupScriptPath;
	}

	@DataBoundSetter
	public void setStartupScriptPath(String startupScriptPath) {
		this.startupScriptPath = startupScriptPath;
	}

	public String getCompilerShortName() {
		return compilerShortName;
	}

	@DataBoundSetter
	public void setCompilerShortName(String compilerShortName) {
		this.compilerShortName = compilerShortName;
	}

	public String getPilConfig() {
		return pilConfig;
	}

	@DataBoundSetter
	public void setPilConfig(String pilConfig) {
		this.pilConfig = pilConfig;
	}

	public int getPilTimeout() {
		return pilTimeout;
	}

	@DataBoundSetter
	public void setPilTimeout(int pilTimetout) {
		this.pilTimeout = pilTimetout;
	}

	public String getMatlabVersion() {
		return matlabVersion;
	}

	@DataBoundSetter
	public void setMatlabVersion(String matlabVersion) {
		this.matlabVersion = matlabVersion;
	}

	public String getMatlabInstancePolicy() {
		return matlabInstancePolicy;
	}

	@DataBoundSetter
	public void setMatlabInstancePolicy(String matlabInstancePolicy) {
		this.matlabInstancePolicy = matlabInstancePolicy;
	}

	public boolean isSaveProfileAfterEachStep() {
		return saveProfileAfterEachStep;
	}

	@DataBoundSetter
	public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
		this.saveProfileAfterEachStep = saveProfileAfterEachStep;
	}

	public String getLicenseLocationString() {
		return licenseLocationString;
	}

	@DataBoundSetter
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
