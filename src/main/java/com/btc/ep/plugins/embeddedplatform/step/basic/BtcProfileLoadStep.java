package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
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
import org.openapitools.client.model.Job;
import org.openapitools.client.model.UpdateModelPath;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileLoadStepExecution extends AbstractBtcStepExecution {

	private static final long serialVersionUID = 1L;
	private BtcProfileLoadStep step;

	public BtcProfileLoadStepExecution(BtcProfileLoadStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	private ProfilesApi profilesApi = new ProfilesApi();
	private ArchitecturesApi archApi = new ArchitecturesApi();

	@Override
	protected void performAction() throws Exception {
		/*
		 * Preliminary checks
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		checkArgument(profilePath != null, "No valid profile path was provided: " + step.getProfilePath());
		Store.epp = resolveInAgentWorkspace(profilePath);
		Store.exportPath = toRemoteAbsolutePathString(step.getExportPath() != null ? step.getExportPath() : "reports")
				.toString();
		/*
		 * Load the profile
		 */
		log("Loading profile '" + Store.epp.getName() + "'");
		String msg = null;
		try {
			profilesApi.openProfile(step.getProfilePath(), true);
			updateModelPaths();
			msg = "Successfully loaded the profile";
			response = 200;
		} catch (Exception e) {
			log("ERROR: failed to open profile. " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
			msg = "Could not open profile";
		}

		try {
			boolean hasModelBasedArchitecture = archApi.getArchitectures(null).stream()
					.filter(arch -> "Simulink".equals(arch.getArchitectureKind())
							|| "TargetLink".equals(arch.getArchitectureKind()))
					.findAny().isPresent();
			if (hasModelBasedArchitecture || step.getStartupScriptPath() != null) {
				Util.configureMatlabConnection(step.getMatlabVersion(), step.getMatlabInstancePolicy());
				// TODO: Execute Startup Script (requires EP-2535)
			}
		} catch (Exception e) {
			log("WARNING. Failed to get existing architectures: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			warning();
		}

		// Compiler Settings
		String compilerShortName = step.getCompilerShortName();
		if (compilerShortName != null) {
			Util.setCompiler(compilerShortName);
		}
		/*
		 * Update architecture if required
		 */
		if (step.isUpdateRequired()) {
			try {
				Job archUpdate = archApi.architectureUpdate();
				log("Updating architecture...");
				HttpRequester.waitForCompletion(archUpdate.getJobID());
				msg += " (incl. arch-update)";
				response = 201;
			} catch (Exception e) {
				log("WARNING. Failed to update architecture: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				warning();
			}
		}
		log(msg + ".");
		detailWithLink(Store.epp.getName(), profilePath.toString());
		info(msg + ".");
		response = 200;
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
		path = toRemoteAbsolutePathString(step.getTlModelPath());
		if (path != null) {
			updateModelPath.setTlModelFile(path);
		}
		path = toRemoteAbsolutePathString(step.getTlScriptPath());
		if (path != null) {
			updateModelPath.setTlInitScript(path);
		}
		path = toRemoteAbsolutePathString(step.getSlModelPath());
		if (path != null) {
			updateModelPath.setSlModelFile(path);
		}
		path = toRemoteAbsolutePathString(step.getSlScriptPath());
		if (path != null) {
			updateModelPath.setSlInitScript(path);
		}
		path = toRemoteAbsolutePathString(step.getAddInfoModelPath());
		if (path != null) {
			updateModelPath.setAddModelInfo(path);
		}
		path = toRemoteAbsolutePathString(step.getCodeModelPath());
		if (path != null) {
			updateModelPath.setEnvironment(path);
		}
		try {
			archApi.updateModelPaths("", updateModelPath);
		} catch (Exception e) {
			log("WARNING failed to update master model: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
		}
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcProfileLoadStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private String exportPath;
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

		/*
		 * This specifies the step name that the the user can use in his Jenkins
		 * Pipeline - for example: btcStartup installPath: 'C:/Program
		 * Files/BTC/ep2.9p0', port: 29267
		 */
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
