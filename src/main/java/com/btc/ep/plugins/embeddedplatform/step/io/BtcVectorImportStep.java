package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
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
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.ImportResult;
import org.openapitools.client.model.ImportStatus;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestRBTestCaseImportInfo;
import org.openapitools.client.model.RestRBTestCaseImportInfo.OverwritePolicyEnum;
import org.openapitools.client.model.RestStimuliVectorImportInfo;

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
class BtcVectorImportStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcVectorImportStep step;

	public BtcVectorImportStepExecution(BtcVectorImportStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		VectorImportExecution exec = new VectorImportExecution(step, logger, getContext());
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class VectorImportExecution extends BtcExecution {

	private static final long serialVersionUID = -140646999640558658L;
	private BtcVectorImportStep step;

	public VectorImportExecution(BtcVectorImportStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		RequirementBasedTestCasesApi rbTestCasesApi = new RequirementBasedTestCasesApi();
		StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
		
		// DEBUG OUTPUT
		// DEBUG OUTPUT
		log("Vector Import: performAction() starts...");
		// DEBUG OUTPUT
		// DEBUG OUTPUT
		
		// TODO: EP-2735
		String fileSuffix = deriveSuffix(step.getVectorFormat());
		// vectorFiles will be an array of files or null
		File importDir = resolveToPath(step.getImportDir()).toFile();
		List<File> vectorFiles = Collections.emptyList();
		if (importDir.exists()) {
			File[] files = importDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.getName() != null && file.getName().toLowerCase().endsWith(fileSuffix);
				}
			});
			vectorFiles = Arrays.asList(files);
		}
		
		// DEBUG OUTPUT
		// DEBUG OUTPUT
		log("Vector Import: files: " + vectorFiles);
		// DEBUG OUTPUT
		// DEBUG OUTPUT
		
		// we shouldn't throw an error if the directory doesn't exist
		if (vectorFiles.isEmpty()) {
			String msg = "Nothing to import.";
			log(msg);
			info(msg);
			skipped();
			return response(300);
		}
		// convert FilePaths to list of strings that represent valid paths on the remote
		// file system
		List<String> vectorFilePaths = vectorFiles.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList());
		Job job = null;
		try {
			log("Importing testcases from '%s'...", importDir.getPath());
			if (step.getVectorKind().equals("TC")) {
				// import test cases
				RestRBTestCaseImportInfo info = new RestRBTestCaseImportInfo();
				info.setOverwritePolicy(OverwritePolicyEnum.OVERWRITE);
				info.setPaths(vectorFilePaths);
				job = rbTestCasesApi.importRBTestCase(info);
				// no format? http://jira.osc.local:8080/browse/EP-2534 --> format is
				// auto-detected based on file extension
			} else { // excel
				// import stimuli vectors
				RestStimuliVectorImportInfo info = new RestStimuliVectorImportInfo();
				info.setOverwritePolicy(org.openapitools.client.model.RestStimuliVectorImportInfo.OverwritePolicyEnum.OVERWRITE);
				info.setPaths(vectorFilePaths);
				/*
				 * info.setVectorKind(step.getVectorKind());
				 * info.setFormat(step.getVectorFormat().toLowerCase()); // according to doc
				 * only takes lowercase info.setDelimiter("Semicolon"); String fUid =
				 * foldersApi.getFoldersByQuery(null, null).get(0).getUid().toString();
				 * info.setFolderUID(fUid);
				 */
				log("Importing stimuli vectors from '%s'...", importDir.getPath());
				job = stimuliVectorsApi.importStimuliVectors(info);
			}
		} catch (Exception e) {
			error("Error while importing test cases.", e);
		}
		ImportResult importResult = HttpRequester.waitForCompletion(job.getJobID(), "result", ImportResult.class);
		try {
			processResult(importResult);
		} catch (Exception e) {
			warning("Failed to parse import results.", e);
		}
		return response(200);
	}

	/**
	 * Processes the results and adapts the reporting status for this step.
	 * 
	 * @param importResult
	 */
	private void processResult(ImportResult importResult) {
		// collect warnings & errors
		int numberOfWarnings = 0;
		int numberOfErrors = 0;
		for (ImportStatus status : importResult.getImportStatus()) {
			if (status.getWarnings() != null && !status.getWarnings().isEmpty()) {
				numberOfWarnings++;
			}
			if (status.getErrors() != null && !status.getErrors().isEmpty()) {
				numberOfErrors++;
			}
		}
		// adapt status
		String msg = "Imported " + importResult.getImportStatus().size() + " test cases.";
		if (numberOfErrors > 0) {
			error();
			msg += " Encountered " + numberOfErrors + " errors.";
		} else {
			if (numberOfWarnings > 0) {
				msg += " Encountered " + numberOfWarnings + " warnings in the process.";
				warning();
			}
		}
		log(msg);
		info(msg);
	}

	/**
	 *
	 *
	 * @param vectorFormat
	 * @return
	 * @throws Exception
	 */
	private String deriveSuffix(String vectorFormat) throws IllegalArgumentException {
		switch (vectorFormat.toUpperCase()) {
		case "TC":
			return ".tc";
		case "EXCEL":
			return ".xlsx";
		case "CSV":
			return ".csv";
		default:
			throw new IllegalArgumentException("Unsupported vector format: " + vectorFormat);
		}

	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcVectorImportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */

	private String importDir;
	private String vectorKind = "TC";
	private String vectorFormat = "TC";

	@DataBoundConstructor
	public BtcVectorImportStep(String importDir) {
		super();
		this.importDir = importDir;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcVectorImportStepExecution(this, context);
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
			return "btcVectorImport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Vector Import";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getImportDir() {
		return importDir;
	}

	public String getVectorKind() {
		return vectorKind;
	}

	@DataBoundSetter
	public void setVectorKind(String vectorKind) {
		this.vectorKind = vectorKind;
	}

	public String getVectorFormat() {
		return vectorFormat;
	}

	@DataBoundSetter
	public void setVectorFormat(String vectorFormat) {
		this.vectorFormat = vectorFormat;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
