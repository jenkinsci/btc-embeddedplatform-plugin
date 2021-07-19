package com.btc.ep.plugins.embeddedplatform.step.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FileFilter;
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
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.StimuliVectorImportInfo;

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
public class BtcVectorImportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String importDir;
    private String vectorFormat = "TC"; //UPDATE THIS ON THE DOCUMENTATION
    private String vectorKind = "TC";

    @DataBoundConstructor
    public BtcVectorImportStep() {
        super();
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
            return "BTC Vector Import Step";
        }
    }

    /**
     * Get importDir.
     * 
     * @return the importDir
     */
    public String getImportDir() {
        return importDir;
    }

    /**
     * Set importDir.
     * 
     * @param importDir the importDir to set
     */
    @DataBoundSetter
    public void setImportDir(String importDir) {
        this.importDir = importDir;
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
class BtcVectorImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcVectorImportStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();

    /**
     * Constructor
     *
     * @param step
     * @param context
     */
    public BtcVectorImportStepExecution(BtcVectorImportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    /*
     * Put the desired action here:
     * - checking preconditions
     * - access step parameters (field step: step.getFoo())
     * - calling EP Rest API
     * - print text to the Jenkins console (field: jenkinsConsole)
     * - set resonse code (field: response)
     */
    @Override
    protected void performAction() throws Exception {

        //Get all vectors
        Path importDir = resolvePath(step.getImportDir());
        checkArgument(importDir.toFile().exists(), "Error: Import directory does not exist " + importDir);

        File[] vectors = importDir.toFile().listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".tc"); // Analyze vector format, use variable
            }
        });
        List<String> paths = new ArrayList<>();
        for (File f : vectors) {
            paths.add(f.getAbsolutePath());
        }

        // Stimuli Vector
        StimuliVectorImportInfo info = new StimuliVectorImportInfo();
        info.setFormat(step.getVectorFormat().toLowerCase());
        info.setVectorKind("TC");
        info.paths(paths);
        stimuliVectorsApi.importStimuliVectors(info);
    }

}
