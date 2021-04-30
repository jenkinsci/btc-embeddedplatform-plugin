package com.btc.ep.plugins.embeddedplatform.step;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
class BtcExampleStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;

    private BtcExampleStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private String status;

    /**
     * Constructor
     *
     * @param btcStartupStep
     * @param context
     */
    public BtcExampleStepExecution(BtcExampleStep btcStartupStep, StepContext context) {
        super(context);
        this.step = btcStartupStep;
    }

    /*
     * The start method must either
     * - start an asychronous process in its own thread (e.g. TimerTask) and then return false or
     * - perform the desired action immediately (shouldn't take more that 1-2 seconds) and then return true
     */
    @Override
    public boolean start() throws Exception {
        System.out.println("BtcExampleStepExecution.start() has been called!"); // this would go to the 'jenkins.out.log' file in JENKINS_HOME

        /*
         * We can use something like this timer task to implement the desired action, i.e.:
         * - process the step parameters
         * - check preconditions
         * -> is the API already connected?
         * -> is EP in the expected state (e.g. profile loaded, architecture imported, test cases available...)?
         * -> do all referenced files exist?
         * - perform action using EP SDK
         */
        TimerTask t = new TimerTask() {

            @Override
            public void run() {
                // This is what's actually executed (currently just prints some text to the Jenkins console):
                try {
                    PrintStream jenkinsConsole = getContext().get(TaskListener.class).getLogger();
                    jenkinsConsole.println("The value of the string parameter is " + step.getStrStepParam());
                    jenkinsConsole.println("The value of the integer parameter is " + step.getIntStepParam());
                    for (int i = 0; i < 5; i++) {
                        jenkinsConsole.println("Hello there! " + i);
                        Thread.sleep(2000);
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                    getContext().onFailure(e1);
                }
                // Important: always call this method (unless onFailure is called), otherwise Jenkins will wait forever
                getContext().onSuccess("Finished");
            }
        };
        // trigger the desired action by calling the run() method
        t.run();
        // return false (see explanation in comment on start() method)
        return false;
    }

    @Override
    public String getStatus() {
        return status;
    }

}
