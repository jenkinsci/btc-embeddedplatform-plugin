package com.btc.ep.plugins.embeddedplatform.step.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.TolerancesApi;
import org.openapitools.client.model.TolerancesImportConfig;

import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcToleranceImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcToleranceImportStep step;

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
    public BtcToleranceImportStepExecution(BtcToleranceImportStep step, StepContext context) {
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
        String path = toRemoteAbsolutePathString(step.getPath());
        TolerancesImportConfig info = new TolerancesImportConfig();
        info.setPath(path);
        if (step.getUseCase().equals("RBT")) {
            // Requirements-based Test
            info.setToleranceUseCase("RBT");
        } else if (step.getUseCase().equals("B2B")) {
            // Back-to-Back Test
            info.setToleranceUseCase("B2B");
        } else {
        	checkArgument(false, "Valid use cases for Tolerance Import are B2B or RBT");
        }
        try {
        	tolerancesApi.setGlobalTolerances(info);
        } catch (Exception e) {
        	log("ERROR: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        info("Imported Tolerances.");

        //HttpRequester.waitForCompletion(job.getJobID());


    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcToleranceImportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String path;
    private String useCase = "B2B";

    @DataBoundConstructor
    public BtcToleranceImportStep(String path) {
        super();
        this.setPath(path);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcToleranceImportStepExecution(this, context);
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
            return "btcToleranceImport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Tolerance Import Step";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /*
     * End of getter/setter section
     */

} // end of step class
