package com.btc.ep.plugins.embeddedplatform.step;

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

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/*
 * ################################################################################################
 * #                                                                                              #
 * #  THIS IS A TEMPLATE: YOU MAY COPY THIS FILE AS A STARTING POINT TO IMPLEMENT FURTHER STEPS.  #
 * #                                                                                              # 
 * ################################################################################################
 */

/**
 * This class defines what happens when the above step is executed
 */
class BtcExampleStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcExampleStep step;

	public BtcExampleStepExecution(BtcExampleStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ExampleExecution exec = new ExampleExecution(step, logger, getContext());
		// if needed: transfer applicable global options from Store to the dataTransferObject to be available on the agent
		// exec.dataTransferObject.matlabVersion = Store.matlabVersion;
		
		// Main Step Execution on the Jenkins AGENT:
		// BtcExecution.call() -> executes ExampleExecution::performAction() 
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	


}

class ExampleExecution extends BtcExecution {
	
	private static final long serialVersionUID = 4101046454592463535L;
	private BtcExampleStep step;

	public ExampleExecution(BtcExampleStep step, PrintStream logger, StepContext context) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}

	/*
	 * This method runs on the agent!
	 * - it has no access to the step context / getContext
	 * - it has no access to global storage units like Store or static helper methods
	 * - relevant helper methods are available from the super-class BtcExecution 
	 */
	@Override
	protected Object performAction() throws Exception {
		log("The value of the string parameter is " + step.getStrStepParam());
		log("The value of the integer parameter is " + step.getIntStepParam());
		// print success message and return response code
		log("--> [200] Example step successfully executed.");
		return response(200);
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcExampleStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String strStepParam;
	private int intStepParam;

	@DataBoundConstructor
	public BtcExampleStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcExampleStepExecution(this, context);
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
			return "btcExample";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Example Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getStrStepParam() {
		return strStepParam;

	}

	@DataBoundSetter
	public void setStrStepParam(String strStepParam) {
		this.strStepParam = strStepParam;

	}

	public int getIntStepParam() {
		return intStepParam;

	}

	@DataBoundSetter
	public void setIntStepParam(int intStepParam) {
		this.intStepParam = intStepParam;

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
