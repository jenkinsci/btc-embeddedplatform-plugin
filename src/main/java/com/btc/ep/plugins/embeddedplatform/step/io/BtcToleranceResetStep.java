package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.openapitools.client.api.TolerancesApi;

import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcToleranceResetStepExecution extends AbstractBtcStepExecution {

	private static final long serialVersionUID = 1L;
	private BtcToleranceResetStep step;

	/**
	 * Constructor
	 *
	 * @param step
	 * @param context
	 */
	public BtcToleranceResetStepExecution(BtcToleranceResetStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	private TolerancesApi tolerancesApi = new TolerancesApi();

	@Override
	protected void performAction() throws Exception {
		tolerancesApi.resetGlobalTolerances(step.getUseCase());
		info("Reset Tolerances.");
		
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcToleranceResetStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String useCase = "B2B";

	@DataBoundConstructor
	public BtcToleranceResetStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcToleranceResetStepExecution(this, context);
	}



	/**
	 * Get useCase.
	 * 
	 * @return the useCase
	 */
	public String getUseCase() {
		return useCase;

	}

	/**
	 * Set useCase.
	 * 
	 * @param useCase the useCase to set
	 */
	public void setUseCase(String useCase) {
		this.useCase = useCase;

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
			return "btcToleranceReset";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Tolerance Reset Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	/*
	 * End of getter/setter section
	 */

} // end of step class
