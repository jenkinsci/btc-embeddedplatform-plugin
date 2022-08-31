package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Path;
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
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestExecutionRecordImportInfo;
import org.openapitools.client.model.TolerancesIOConfig;
import org.openapitools.client.model.TolerancesIOConfig.ToleranceUseCaseEnum;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcExecutionRecordImportStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcExecutionRecordImportStep step;

	public BtcExecutionRecordImportStepExecution(BtcExecutionRecordImportStep step, StepContext context) {
		super(context);
		this.step = step;
	}
	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ExecutionRecordImport exec = new ExecutionRecordImport(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	
}

class ExecutionRecordImport extends BtcExecution {
	
	private static final long serialVersionUID = -1620879242049841611L;
	transient ExecutionRecordsApi erApi = new ExecutionRecordsApi();
	transient ExecutionConfigsApi ecApi = new ExecutionConfigsApi();
	private BtcExecutionRecordImportStep step;
	public ExecutionRecordImport(PrintStream logger, StepContext context, BtcExecutionRecordImportStep step) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		FilePath exportDir = new FilePath(resolveToPath(step.getDir()).toFile());

		List<FilePath> files = exportDir.list((f) -> f.getName().endsWith(".mdf"));
		List<String> paths = files.stream().map(fp -> fp.getRemote()).collect(Collectors.toList());
		RestExecutionRecordImportInfo data = new RestExecutionRecordImportInfo();
		// execution config can be user-defined, so there's no check to make
		String kind = step.getExecutionConfig() != null ? step.getExecutionConfig()
				: ecApi.getExecutionConfigs().getExecConfigNames().get(0);
		data.setKind(kind);
		data.setPaths(paths);
		data.setFolderName(step.getFolderName());
		Job job = null;
		Object response_obj = null;
		try {
			job = erApi.importExecutionRecord(data);
			response_obj = HttpRequester.waitForCompletion(job.getJobID(), "statusCode");
		} catch (Exception e) {
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
		}
		int response_int = (int) response_obj;
		switch (step.getExecutionConfig()) {
		case "TL MIL":
		case "SL MIL":
		case "PIL":
		case "SIL":
			break;
		default:
			log("Warning: non-standard execution config " + step.getExecutionConfig()
					+ ". Default options are TL MIL, SL MIL, PIL, and SIL. Make sure this isn't a typo!");
			warning();
		}
		int response = response_int;
		switch (response_int) {
		case 201:
			// successful. nothing to report.
			break;
		case 400:
			log("Error: Bad request (make sure the arguments you passed in are valid");
			error();
			break;
		case 404:
			log("Error: Not found.");
			error();
			break;
		case 500:
			log("Error: Internal server error.");
			error();
			break;
		}
		info("Finished important execution records");
		return null;

	}


}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcExecutionRecordImportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String dir;
	private String executionConfig;
	private String folderName;

	@DataBoundConstructor
	public BtcExecutionRecordImportStep(String dir) {
		super();
		this.dir = dir;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcExecutionRecordImportStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcExecutionRecordImport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Execution Record Import Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */
	public String getDir() {
		return dir;
	}

	public String getExecutionConfig() {
		return executionConfig;
	}

	public String getFolderName() {
		return folderName;

	}

	@DataBoundSetter
	public void setFolderName(String folderName) {
		this.folderName = folderName;

	}
	/*
	 * End of getter/setter section
	 */

} // end of step class
