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
import org.openapitools.client.api.ApplicationApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.ProfilePath;

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcWrapUpStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcWrapUpStep step;

	public BtcWrapUpStepExecution(BtcWrapUpStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		WrapUpExecution exec = new WrapUpExecution(step, logger, getContext());

		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// Generate the project report and xml report
		StepExecutionHelper.assembleProjectReport(getContext(), logger);
		StepExecutionHelper.archiveArtifacts(getContext());
		StepExecutionHelper.exportJUnitReport(getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	
}

/**
 * This class defines what happens when the above step is executed
 */
class WrapUpExecution extends BtcExecution {

	private static final long serialVersionUID = -5603806953855309282L;
	private BtcWrapUpStep step;

	public WrapUpExecution(BtcWrapUpStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		ProfilesApi profileApi = new ProfilesApi();
		ApplicationApi applicationApi = new ApplicationApi();
		/*
		 * Save the profile
		 */
		String profilePath = getProfilePathOrDefault(step.getProfilePath());
		// save the epp to the designated location
		log("Saving to " + profilePath);
		profileApi.saveProfile(new ProfilePath().path(profilePath));

		/*
		 * Exit the application (first softly via API)
		 */
		if (step.isCloseEp()) {
			try {
				applicationApi.exitApplication(true);
			} catch (Exception e) { // doesn't really matter what we do, as long as we dont crash
			}
			try {
				// hard kill to be on the save side
				if (Store.epProcess != null && Store.epProcess.isAlive()) {
					// ... und bist du nicht willig, so brauch ich Gewalt!
					Store.epProcess.kill();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			log("Successfully closed BTC EmbeddedPlatform.");
		}
		return response(200);
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcWrapUpStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String profilePath;
	private boolean closeEp = true;

	@DataBoundConstructor
	public BtcWrapUpStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcWrapUpStepExecution(this, context);
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
			return "btcWrapUp";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Wrap Up Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getProfilePath() {
		return profilePath;

	}

	@DataBoundSetter
	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;

	}

	public boolean isCloseEp() {
		return closeEp;

	}

	@DataBoundSetter
	public void setCloseEp(boolean closeEp) {
		this.closeEp = closeEp;

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
