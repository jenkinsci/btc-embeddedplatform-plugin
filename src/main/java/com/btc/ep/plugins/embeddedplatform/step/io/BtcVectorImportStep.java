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
import org.openapitools.client.api.FoldersApi;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RBTestCaseImportInfo;
import org.openapitools.client.model.StimuliVectorImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.google.gson.internal.LinkedTreeMap;

import hudson.Extension;
import hudson.model.TaskListener;

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
    private String vectorFormat = "excel";

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

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcVectorImportStep step;
    private static final String OVERWRITE = "OVERWRITE"; // for overwrite policy on tc import

    private RequirementBasedTestCasesApi rbTestCasesApi = new RequirementBasedTestCasesApi();
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
    private FoldersApi foldersApi = new FoldersApi();

    public BtcVectorImportStepExecution(BtcVectorImportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected void performAction() throws Exception {
        String fileSuffix = deriveSuffix(step.getVectorFormat());
        Path importDir = resolvePath(step.getImportDir());
        checkArgument(importDir != null && importDir.toFile().exists(), "Import directory not available: " + importDir);

        File[] vectorFiles = importDir.toFile().listFiles((f) -> f.getName().endsWith(fileSuffix));
        List<String> vectorFilePaths = new ArrayList<>();
        for (File vectorFile : vectorFiles) {
            vectorFilePaths.add(vectorFile.getAbsolutePath());
        }
        Job job;
        if (step.getVectorKind().equals("TC")) {
            // import test cases
            RBTestCaseImportInfo info = new RBTestCaseImportInfo();
            info.setOverwritePolicy(OVERWRITE);
            info.setPaths(vectorFilePaths);
            // no format? http://jira.osc.local:8080/browse/EP-2534 --> format is auto-detected based on file extension
            job = rbTestCasesApi.importRBTestCase(info);
        } else {
            // import stimuli vectors
            StimuliVectorImportInfo info = new StimuliVectorImportInfo();
            info.setOverwritePolicy(OVERWRITE);
            info.setPaths(vectorFilePaths);
           /* info.setVectorKind(step.getVectorKind());
            info.setFormat(step.getVectorFormat().toLowerCase()); // according to doc only takes lowercase
            info.setDelimiter("Semicolon");
            String fUid = foldersApi.getFoldersByQuery(null, null).get(0).getUid().toString();
            info.setFolderUID(fUid);*/
            job = stimuliVectorsApi.importStimuliVectors(info);
        }
        Object status = HttpRequester.waitForCompletion(job.getJobID(), "result");
        if (status == null) {
        	// i think the callback will at least return an error message? so we should never be here.
        	// but might as well throw it in just in case i'm wrong
        	log("Something has gone horribly wrong in importing the vectors");
        	return;
        }
        @SuppressWarnings("unchecked")
        LinkedTreeMap<String, ArrayList<LinkedTreeMap<String, ArrayList<String>>>> status_l1 = ((LinkedTreeMap<String, ArrayList<LinkedTreeMap<String, ArrayList<String>>>>) status);
        ArrayList<LinkedTreeMap<String, ArrayList<String>>> status_l2 = status_l1.get("importStatus");
        String returnString = null;
        try {
        	status_l2.get(0).get("UIDs");
        } catch (Exception e) {
        	// no UIDs. this is probably an issue
        	returnString += "No UIDs to import into\n";
        	
        }
        try {
        	ArrayList<String> status_l32 = status_l2.get(0).get("warnings");
        	returnString += status_l32.toString();
        } catch (Exception e) {
        	// no warnings. dont need to do anything
        }
        try {
        	ArrayList<String> status_l34 = status_l2.get(0).get("errors");
        	returnString += status_l34.toString();
        } catch (Exception e) {
        	// no warnings. dont need to do anything
        }
        if (returnString != null) {
        	System.out.println(returnString);
        	info(returnString);
        }
        else {
        	System.out.println("Successfully imported test cases with no errors");
        }
        
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
