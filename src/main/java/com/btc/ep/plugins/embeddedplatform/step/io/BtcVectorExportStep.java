package com.btc.ep.plugins.embeddedplatform.step.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestRBTestCaseExportInfo;
import org.openapitools.client.model.RestStimuliVectorExportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/*
 * ################################################################################################
 * #                                                                                              #
 * #     THIS IS A TEMPLATE: COPY THIS FILE AS A STARTING POINT TO IMPLEMENT FURTHER STEPS.       #
 * #                                                                                              # 
 * ################################################################################################
 */

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcVectorExportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String dir;
    private String vectorFormat = "TC"; //UPDATE THIS ON THE DOCUMENTATION
    private String vectorKind = "TC";

    @DataBoundConstructor
    public BtcVectorExportStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcVectorExportStepExecution(this, context);
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
            return "btcVectorExport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Vector Export Step";
        }
    }

    /**
     * Get exportDir.
     * 
     * @return the exportDir
     */
    public String getExportDir() {
        return dir;
    }

    /**
     * Set exportDir.
     * 
     * @param exportDir the exportDir to set
     */
    @DataBoundSetter
    public void setExportDir(String exportDir) {
        this.dir = exportDir;
    }

    /**
     * Get vectorFormat.
     * 
     * @return the vectorFormat
     */
    public String getVectorFormat() {
        return vectorFormat;
    }

    /**
     * Set vectorFormat.
     * 
     * @param vectorFormat the vectorFormat to set
     */
    @DataBoundSetter
    public void setVectorFormat(String vectorFormat) {
        this.vectorFormat = vectorFormat;
    }

    /**
     * Get vectorKind.
     * 
     * @return the vectorKind
     */
    public String getVectorKind() {
        return vectorKind;
    }

    /**
     * Set vectorKind.
     * 
     * @param vectorKind the vectorKind to set
     */
    @DataBoundSetter
    public void setVectorKind(String vectorKind) {
        this.vectorKind = vectorKind;
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorExportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcVectorExportStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
    private RequirementBasedTestCasesApi rbTestCaseApi = new RequirementBasedTestCasesApi();

    /**
     * Constructor
     *
     * @param step
     * @param context
     */
    public BtcVectorExportStepExecution(BtcVectorExportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    /*
     * Put the desired action here:
     * - checking preconditions
     * - access step parameters (field step: step.getFoo())
     * - calling EP Rest API
     * - print text to the Jenkins console (field: jenkinsConsole)
     * - set response code (field: response)
     */
    @Override
    protected void performAction() throws Exception {

        //Get all vectors
        Path exportDir = resolvePath(step.getExportDir());
        checkArgument(exportDir.toFile().exists(), "Error: Export directory does not exist " + exportDir);

        // Stimuli Vector  -- Default info.setVectorKind("TC");
        Job job;
        if (step.getVectorKind().equals("SV")) {
            // Stimuli Vector
            RestStimuliVectorExportInfo info = new RestStimuliVectorExportInfo();
            info.setExportFormat(step.getVectorFormat().toLowerCase());
            info.setExportDirectory(exportDir.toString());
            //info.setUiDs(); //TODO NEED TO GET ALL UIDS FOR EXPORT
            job = stimuliVectorsApi.exportStimuliVectors(info);
            info("Exported Stimuli Vectors.");
        } else {
            // Test Case
            RestRBTestCaseExportInfo info = new RestRBTestCaseExportInfo();
            info.setExportFormat(step.getVectorFormat().toLowerCase());
            info.setExportDirectory(exportDir.toString());
            //info.setUiDs(); //TODO NEED TO GET ALL UIDS FOR EXPORT
            job = rbTestCaseApi.exportRBTestCases(info);
            info("Imported Test Cases.");
        }
        HttpRequester.waitForCompletion(job.getJobID());

        // Questions
        // 1. How should I handle the job from the stimuliVectorsApi.importStimuliVectors(info)?
        // 2. For the Stimuli Vector, do I just need to switch the setVectorKind, or do I need to use another Api?
        // I couldn't find an api for Test Cases

    }

    private String findFileExtension(String format) {
        String fileExtension;
        if (format.equals("TC")) {
            fileExtension = ".tc";
        } else if (format.equals("CSV")) {
            fileExtension = ".csv";
        } else if (format.equals("EXCEL")) {
            fileExtension = ".xlsx";
        } else {
            // Error, format not recognized
            fileExtension = ".tc";
        }
        return fileExtension;
    }

}