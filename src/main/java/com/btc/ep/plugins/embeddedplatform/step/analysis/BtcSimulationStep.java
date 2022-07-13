package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ExecutionConfigsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.api.TestCaseStimuliVectorSimulationApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.TestCaseSimulationOnListParams;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcSimulationStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcSimulationStep step;
	
	public BtcSimulationStepExecution(BtcSimulationStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		SimulationExec exec = new SimulationExec(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		//exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class SimulationExec extends BtcExecution {
	
	private BtcSimulationStep step;
	private ScopesApi scopeApi = new ScopesApi();
	private ExecutionConfigsApi ecApi = new ExecutionConfigsApi();
	private TestCaseStimuliVectorSimulationApi simApi = new TestCaseStimuliVectorSimulationApi();
	
	
	public SimulationExec(PrintStream logger, StepContext context, BtcSimulationStep step) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		// Prepare data and simulate
		try {
			TestCaseSimulationOnListParams info = prepareInfoObject();
			Job job = simApi.simulateOnScopeList(info);
			HttpRequester.waitForCompletion(job.getJobID());
		} catch (Exception e) {
			error("Failed simulate vectors.", e);
			return null;
		}
		log("--> Simulation successfully executed.");
		return null;
	}

	private TestCaseSimulationOnListParams prepareInfoObject() throws ApiException {
		TestCaseSimulationOnListParams info = new TestCaseSimulationOnListParams();
		List<String> scopeUids = scopeApi.getScopesByQuery1(null, FALSE).stream().map(scope -> scope.getUid())
				.collect(Collectors.toList());
		List<String> executionConfigNames = Util.getValuesFromCsv(step.getExecutionConfigString());
		if (executionConfigNames.isEmpty()) {
			executionConfigNames = ecApi.getExecutionConfigs().getExecConfigNames();
		}
		log("Simulating on %s...", executionConfigNames);
		info.setExecConfigNames(executionConfigNames);
		info.setUiDs(scopeUids);
		return info;
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcSimulationStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String executionConfigString;

	@DataBoundConstructor
	public BtcSimulationStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcSimulationStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcSimulation";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Simulation Step";
		}
	}

	public String getExecutionConfigString() {
		return executionConfigString;
	}

	@DataBoundSetter
	public void setExecutionConfigString(String executionConfigString) {
		this.executionConfigString = executionConfigString;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
