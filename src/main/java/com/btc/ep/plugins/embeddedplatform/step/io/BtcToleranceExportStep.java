package com.btc.ep.plugins.embeddedplatform.step.io;

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
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.api.TolerancesApi;

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorExportStepExecution.VectorExportExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcToleranceExportStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;

	private BtcToleranceExportStep step;

	/*
	 * This field can be used to indicate what's happening during the execution
	 */
	private TolerancesApi tolerancesApi = new TolerancesApi();
	private ScopesApi scopesApi = new ScopesApi();

	/**
	 * Constructor
	 *
	 * @param step
	 * @param context
	 */
	public BtcToleranceExportStepExecution(BtcToleranceExportStep step, StepContext context) {
		super(context);
		this.step = step;
	}
	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ToleranceExport exec = new ToleranceExport(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}
class ToleranceExport extends BtcExecution {

	private static final long serialVersionUID = 4612932724469604052L;
	private BtcToleranceExportStep step;
	public ToleranceExport(PrintStream logger, StepContext context, BtcToleranceExportStep step) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}
	

	@Override
	protected Object performAction() throws Exception {
		// Get the path
		String path = step.getPath() != null ? resolveToString(step.getPath()) : Store.exportPath;
		String kind = step.getUseCase();
		checkArgument(kind == "RBT" || kind == "B2B",
				"Error: invalid use case '" + kind + "'. Supported cases are 'RBT' and 'B2B'.");

		// TODO: ep-2723. this is a temporary workaround in the meantime.
		return null;
	}


}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcToleranceExportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String path;
	private String useCase = "B2B";

	@DataBoundConstructor
	public BtcToleranceExportStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcToleranceExportStepExecution(this, context);
	}

	/**
	 * Get path.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;

	}

	/**
	 * Set path.
	 * 
	 * @param path the path to set
	 */
	@DataBoundSetter
	public void setPath(String path) {
		this.path = path;

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
	@DataBoundSetter
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
			return "btcToleranceExport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Tolerance Export Step";
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
