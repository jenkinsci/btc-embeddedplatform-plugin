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
import org.openapitools.client.api.TolerancesApi;

import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcToleranceExportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String path;
    private String useCase = "B2B";

    @DataBoundConstructor
    public BtcToleranceExportStep(String path) {
        super();
        this.setPath(path);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcToleranceExportStepExecution(this, context);
    }

    /**
     * Get path.
     * 
     * @return the path
     */
    public String getPath() {
        return path;

    }

    /**
     * Set path.
     * 
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;

    }

    /**
     * Get useCase.
     * 
     * @return the useCase
     */
    public String getUseCase() {
        return useCase;

    }

    /**
     * Set useCase.
     * 
     * @param useCase the useCase to set
     */
    public void setUseCase(String useCase) {
        this.useCase = useCase;

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
            return "btcToleranceExport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Tolerance Export Step";
        }
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
class BtcToleranceExportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcToleranceExportStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private TolerancesApi tolerancesApi = new TolerancesApi();

    /**
     * Constructor
     *
     * @param step
     * @param context
     */
    public BtcToleranceExportStepExecution(BtcToleranceExportStep step, StepContext context) {
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

        // Get the path
        Path path = resolvePath(step.getPath());
        checkArgument(path.toFile().exists(), "Error: Export xml does not exist " + path);

        // Goal:
        // Take path, make sure xml file exists
        // Write the tolerances to this xml file
        // Matlab API has tolerance export

    }

}
