package com.btc.ep.plugins.embeddedplatform.step.io;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.FoldersApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.ImportResult;
import org.openapitools.client.model.ImportStatus;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RBTestCaseImportInfo;
import org.openapitools.client.model.StimuliVectorImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcVectorImportStep step;
    private static final String OVERWRITE = "OVERWRITE"; // for overwrite policy on tc import

    private ProfilesApi profilesApi = new ProfilesApi();
    private RequirementBasedTestCasesApi rbTestCasesApi = new RequirementBasedTestCasesApi();
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
    private FoldersApi foldersApi = new FoldersApi();

    public BtcVectorImportStepExecution(BtcVectorImportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected void performAction() throws Exception {
    	// Check preconditions
        try {
            profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
        	response = 500;
        	result("ERROR");
            throw new IllegalStateException("You need an active profile for the current command");
        }
        // TODO: EP-2735
        String kind = step.getVectorKind();
        String format = step.getVectorFormat();
        checkArgument(kind == "TC" || kind == "Excel",
        		"Error: valid vectorKind is TC or Excel, not " + kind);
        checkArgument(format == "csv" || format == "excel", 
        		"Error: valid vectorFormat is csv or excel, not " + format);
        
        String fileSuffix = deriveSuffix(step.getVectorFormat());
        Path importDir = resolvePath(step.getImportDir());
    	// vectorFiles will be an array of files or null
        File[] vectorFiles = importDir.toFile().exists() ? importDir.toFile().listFiles((f) -> f.getName().toLowerCase().endsWith(fileSuffix.toLowerCase())) : null;

        // we shouldn't throw an error if the directory doesn't exist 
        if (vectorFiles == null || vectorFiles.length == 0) {
        	String msg = "Nothing to import.";
        	log(msg);
        	info(msg);
        	skipped();
        	return;
        }
        List<String> vectorFilePaths = new ArrayList<>();
        for (File vectorFile : vectorFiles) {
            vectorFilePaths.add(vectorFile.getAbsolutePath());
        }
        Job job = null;
        if (step.getVectorKind().equals("TC")) {
            // import test cases
            RBTestCaseImportInfo info = new RBTestCaseImportInfo();
            info.setOverwritePolicy(OVERWRITE);
            info.setPaths(vectorFilePaths);
            // no format? http://jira.osc.local:8080/browse/EP-2534 --> format is auto-detected based on file extension
            try {
            	job = rbTestCasesApi.importRBTestCase(info);
            } catch (Exception e) {
            	log("ERROR importing RBT: " + e.getMessage());
            	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
            	error();
            }
        } else { // excel
            // import stimuli vectors
            StimuliVectorImportInfo info = new StimuliVectorImportInfo();
            info.setOverwritePolicy(OVERWRITE);
            info.setPaths(vectorFilePaths);
           /* info.setVectorKind(step.getVectorKind());
            info.setFormat(step.getVectorFormat().toLowerCase()); // according to doc only takes lowercase
            info.setDelimiter("Semicolon");
            String fUid = foldersApi.getFoldersByQuery(null, null).get(0).getUid().toString();
            info.setFolderUID(fUid);*/
            try {
            	job = stimuliVectorsApi.importStimuliVectors(info);
            } catch (Exception e) {
            	log("ERROR importing Stimul Vectors: " + e.getMessage());
            	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
            	error();
            }
        }
        ImportResult importResult = HttpRequester.waitForCompletion(job.getJobID(), "result", ImportResult.class);
        try {
        	processResult(importResult);
        } catch (Exception e) {
        	log("WARNING failed to parse import results: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	warning();
        }
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
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
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
         * This specifies the step name that the the user can use in his Jenkins Pipeline
         * - for example: btcStartup installPath: 'C:/Program Files/BTC/ep2.9p0', port: 29267
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
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
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
