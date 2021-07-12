package com.btc.ep.plugins.embeddedplatform.step;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
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
public class BtcExampleStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String strStepParam;
    private int intStepParam;

    @DataBoundConstructor
    public BtcExampleStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcExampleStepExecution(this, context);
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

    /**
     * Get strStepParam.
     * 
     * @return the strStepParam
     */
    public String getStrStepParam() {
        return strStepParam;

    }

    /**
     * Set strStepParam.
     * 
     * @param strStepParam the strStepParam to set
     */
    @DataBoundSetter
    public void setStrStepParam(String strStepParam) {
        this.strStepParam = strStepParam;

    }

    /**
     * Get intStepParam.
     * 
     * @return the intStepParam
     */
    public int getIntStepParam() {
        return intStepParam;

    }

    /**
     * Set intStepParam.
     * 
     * @param intStepParam the intStepParam to set
     */
    @DataBoundSetter
    public void setIntStepParam(int intStepParam) {
        this.intStepParam = intStepParam;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcExampleStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcExampleStep step;

    public BtcExampleStepExecution(BtcExampleStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
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
        jenkinsConsole.println("The value of the string parameter is " + step.getStrStepParam());
        jenkinsConsole.println("The value of the integer parameter is " + step.getIntStepParam());
        jenkinsConsole.println("Workspace: " + Paths.get(getContext().get(FilePath.class).toURI()).toString());
        // print success message and return response code
        jenkinsConsole.println("--> [200] Example step successfully executed.");
        response = 200;
    }

}
