package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ExecutionConfigsApi;
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestExecutionRecordImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcExecutionRecordImportStepExecution extends StepExecution {

	private static final long serialVersionUID = 1L;
	private BtcExecutionRecordImportStep step;

	public BtcExecutionRecordImportStepExecution(BtcExecutionRecordImportStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	private ExecutionRecordsApi erApi = new ExecutionRecordsApi();
	private ExecutionConfigsApi ecApi = new ExecutionConfigsApi();

//	@Override
//	protected void performAction() throws Exception {
//		FilePath exportDir = resolveInAgentWorkspace(step.getDir());
//
//		List<FilePath> files = exportDir.list((f) -> f.getName().endsWith(".mdf"));
//		List<String> paths = files.stream().map(fp -> fp.getRemote()).collect(Collectors.toList());
//		RestExecutionRecordImportInfo data = new RestExecutionRecordImportInfo();
//		// execution config can be user-defined, so there's no check to make
//		String kind = step.getExecutionConfig() != null ? step.getExecutionConfig()
//				: ecApi.getExecutionConfigs().getExecConfigNames().get(0);
//		data.setKind(kind);
//		data.setPaths(paths);
//		data.setFolderName(step.getFolderName());
//		Job job = null;
//		Object response_obj = null;
//		try {
//			job = erApi.importExecutionRecord(data);
//			response_obj = HttpRequester.waitForCompletion(job.getJobID(), "statusCode");
//		} catch (Exception e) {
//			try {
//				log(((ApiException) e).getResponseBody());
//			} catch (Exception idc) {
//			}
//			;
//		}
//		int response_int = (int) response_obj;
//		switch (step.getExecutionConfig()) {
//		case "TL MIL":
//		case "SL MIL":
//		case "PIL":
//		case "SIL":
//			break;
//		default:
//			log("Warning: non-standard execution config " + step.getExecutionConfig()
//					+ ". Default options are TL MIL, SL MIL, PIL, and SIL. Make sure this isn't a typo!");
//			warning();
//		}
//		response = response_int;
//		switch (response_int) {
//		case 201:
//			// successful. nothing to report.
//			break;
//		case 400:
//			log("Error: Bad request (make sure the arguments you passed in are valid");
//			error();
//			break;
//		case 404:
//			log("Error: Not found.");
//			error();
//			break;
//		case 500:
//			log("Error: Internal server error.");
//			error();
//			break;
//		}
//		info("Finished important execution records");
//
//	}

	@Override
	public boolean start() throws Exception {
		// TODO Auto-generated method stub
		return false;
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
