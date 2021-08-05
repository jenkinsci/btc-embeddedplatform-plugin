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
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RBTestCaseImportInfo;
import org.openapitools.client.model.StimuliVectorImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

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
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    public String getImportDir() {
        return importDir;
    }

    public String getVectorKind() {
        return vectorKind;
    }

    public void setVectorKind(String vectorKind) {
        this.vectorKind = vectorKind;
    }

    public String getVectorFormat() {
        return vectorFormat;
    }

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

    private RequirementBasedTestCasesApi rbTestCasesApi = new RequirementBasedTestCasesApi();
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();

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
        if (step.getVectorKind().toUpperCase().equals("TC")) {
            // import test cases
            RBTestCaseImportInfo info = new RBTestCaseImportInfo();
            info.setOverwritePolicy("OVERWRITE");
            info.setPaths(vectorFilePaths);
            // no format??? check REST API
            // http://jira.osc.local:8080/browse/EP-2534
            job = rbTestCasesApi.importRBTestCase(info);
        } else {
            // import stimuli vectors
            StimuliVectorImportInfo info = new StimuliVectorImportInfo();
            info.setOverwritePolicy("OVERWRITE");
            info.setPaths(vectorFilePaths);
            info.setFormat(step.getVectorFormat().toLowerCase()); //needs to be lower case according to docs
            job = stimuliVectorsApi.importStimuliVectors(info);
        }
        Object status = HttpRequester.waitForCompletion(job.getJobID(), "importStatus");
        System.out.println(status);
        //TODO: parse importStatus and adapt response accordingly
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
