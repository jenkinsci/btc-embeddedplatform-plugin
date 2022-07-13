package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.openapitools.client.api.FoldersApi;
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
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestCase;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.FilePath;
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
		RBTExecution exec = new RBTExecution(step, logger, getContext());
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		//exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());

		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class RBTExecution extends BtcExecution {

	private static final long serialVersionUID = -140646999640558658L;
	private BtcVectorImportStep step;

	private static final String OVERWRITE = "OVERWRITE"; // for overwrite policy on tc import
	
	private RequirementBasedTestCasesApi rbTestCasesApi = new RequirementBasedTestCasesApi();
	private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
	private FoldersApi foldersApi = new FoldersApi();

	public RBTExecution(BtcVectorImportStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		// TODO: EP-2735
		String fileSuffix = deriveSuffix(step.getVectorFormat());
		// vectorFiles will be an array of files or null
		FilePath importDir = resolveInAgentWorkspace(step.getImportDir());
		List<FilePath> vectorFiles = new ArrayList<>();
		if (importDir.exists()) {
			List<FilePath> files = importDir.list();
			for (FilePath file : files) {
				if (file.getName() != null && file.getName().toLowerCase().endsWith(fileSuffix)) {
					vectorFiles.add(file);
				}
			}
		}
		// we shouldn't throw an error if the directory doesn't exist
		if (vectorFiles.isEmpty()) {
			String msg = "Nothing to import.";
			log(msg);
			info(msg);
			skipped();
			return null;
		}
		// convert FilePaths to list of strings that represent valid paths on the remote
		// file system
		List<String> vectorFilePaths = vectorFiles.stream().map(fp -> fp.getRemote()).collect(Collectors.toList());
		Job job = null;
		if (step.getVectorKind().equals("TC")) {
			// import test cases
			RestRBTestCaseImportInfo info = new RestRBTestCaseImportInfo();
			info.setOverwritePolicy(OverwritePolicyEnum.OVERWRITE);
			info.setPaths(vectorFilePaths);
			// no format? http://jira.osc.local:8080/browse/EP-2534 --> format is
			// auto-detected based on file extension
			try {
				log("Importing testcases from '%s'...", importDir.getRemote());
				job = rbTestCasesApi.importRBTestCase(info);
			} catch (Exception e) {
				log("ERROR importing RBT: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				error();
			}
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
			try {
				log("Importing stimuli vectors from '%s'...", importDir.getRemote());
				job = stimuliVectorsApi.importStimuliVectors(info);
			} catch (Exception e) {
				log("ERROR importing Stimul Vectors: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				error();
			}
		}
		ImportResult importResult = HttpRequester.waitForCompletion(job.getJobID(), "result", ImportResult.class);
		try {
			processResult(importResult);
		} catch (Exception e) {
			log("WARNING failed to parse import results: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			warning();
		}
		return null;
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
