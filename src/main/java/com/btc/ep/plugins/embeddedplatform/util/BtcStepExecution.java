package com.btc.ep.plugins.embeddedplatform.util;

import java.io.PrintStream;
import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.openapitools.client.ApiException;

import hudson.model.TaskListener;

public abstract class BtcStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;
    protected String status;
    protected PrintStream jenkinsConsole;
    /**
     * Response returned to Jenkins (may be overwritten by implementation)
     */
    protected Object response;
    private String functionName;

    /*
     * --------------- ORIGINAL: START ---------------
     */
    public BtcStepExecution(Step step, StepContext context) {
        super(context);
        this.functionName = step.getDescriptor().getFunctionName();
    }
    /*
     * -------------- ORIGINAL: END -----------------
     */

    /*
     * --------------- Testing: START -----------------
     */
    //    public BtcStepExecution(Step step, StepContext context) {
    //        this.context = context;
    //        this.functionName = "DUMMY";
    //    }
    //
    //    private StepContext context;
    //
    //    @Override
    //    public StepContext getContext() {
    //        return this.context;
    //    }
    /*
     * --------------- Testing: END -----------------
     */

    public boolean start() throws Exception {
        TimerTask t = new TimerTask() {

            @Override
            public void run() {
                try {
                    jenkinsConsole = getContext().get(TaskListener.class).getLogger();
                    // call run method implemented by the individual step
                    performAction();
                    getContext().onSuccess(response); // fallback, if the implementation forgot this
                } catch (Exception e) {
                    if (e instanceof ApiException && jenkinsConsole != null) {
                        String responseBody = ((ApiException)e).getResponseBody();
                        jenkinsConsole.println(
                            "Error during call of " + functionName + "(): " + responseBody);
                    }
                    getContext().onFailure(e);
                }

            }

        };
        t.run();
        return false;
    }

    @Override
    public String getStatus() {
        return status;
    }

    /**
     * Implemented by the individual btc step executors.
     * 
     * @throws Exception
     */
    protected abstract void performAction() throws Exception;
}
