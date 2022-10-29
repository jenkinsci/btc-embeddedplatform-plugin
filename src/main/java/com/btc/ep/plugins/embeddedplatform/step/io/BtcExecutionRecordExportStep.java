package com.btc.ep.plugins.embeddedplatform.step.io;

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
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestExecutionRecordExportInfo;
import org.openapitools.client.model.RestExecutionRecordExportInfo.ExportFormatEnum;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcExecutionRecordExportStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcExecutionRecordExportStep step;

	public BtcExecutionRecordExportStepExecution(BtcExecutionRecordExportStep step, StepContext context) {
		super(context);
		this.step = step;
	}
	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		ExecutionRecordExport exec = new ExecutionRecordExport(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}


class ExecutionRecordExport extends BtcExecution {
	
	

	private static final long serialVersionUID = 4934881457669048090L;

	public ExecutionRecordExport(PrintStream logger, StepContext context, BtcExecutionRecordExportStep step) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}

	private BtcExecutionRecordExportStep step;
	transient ExecutionRecordsApi erApi = new ExecutionRecordsApi();

	@Override
	protected Object performAction() throws Exception {
		String exportDir = step.getDir() != null ? resolveToString(step.getDir()) : Store.exportPath;
		List<String> uids = null;
		try {
			uids = erApi.getExecutionRecords().stream()
					.filter(er -> step.getExecutionConfig().equalsIgnoreCase(er.getExecutionConfig())
							&& (step.getFolderName() == null || step.getFolderName().equals(er.getFolderName())))
					.map(er -> er.getUid()).collect(Collectors.toList());
		} catch (Exception e) {
			log("ERROR. Failed to process execution records: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
		}
		if (uids.isEmpty()) {
			log("Warning: no execution records to export found. Did you run any tests yet?");
			warning();
			return null;
		}

		RestExecutionRecordExportInfo data = new RestExecutionRecordExportInfo();
		data.setUiDs(uids);
		data.setExportDirectory(exportDir);
		data.setExportFormat(ExportFormatEnum.MDF);
		try {
			Job job = erApi.exportExecutionRecords(data);
			Object response = HttpRequester.waitForCompletion(job.getJobID());
			// TODO: the callback is always just null. is there a way of checking the status
			// of the job?
			detailWithLink("Execution Records Export Folder", data.getExportDirectory());
			// TODO: does linking to a folder work? if not just info the export dir.
			info("Exported execution records");
		} catch (Exception e) {
			log("ERROR. Could not export execution records: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
		}
		return null;
	}
	


}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcExecutionRecordExportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String dir;
	private String executionConfig;
	private String folderName;

	@DataBoundConstructor
	public BtcExecutionRecordExportStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcExecutionRecordExportStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcExecutionRecordExport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Execution Record Export Step";
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

	@DataBoundSetter
	public void setDir(String dir) {
		this.dir = dir;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
