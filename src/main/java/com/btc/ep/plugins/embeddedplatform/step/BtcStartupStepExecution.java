package com.btc.ep.plugins.embeddedplatform.step;

import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import hudson.model.TaskListener;

public class BtcStartupStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param btcStartupStep
     * @param context
     */
    public BtcStartupStepExecution(Step btcStartupStep, StepContext context) {
        super(context);
    }

    @Override
    public boolean start() throws Exception {
        System.out.println("BtcStartupStepExecution.start() has been called!");
        TimerTask t = new TimerTask() {

            @Override
            public void run() {
                // This is what's actually executed (currently just prints some text to the Jenkins console):
                try {
                    TaskListener listener = getContext().get(TaskListener.class);
                    for (int i = 0; i < 5; i++) {
                        listener.getLogger().println("Hello there! " + i);
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                getContext().onSuccess("This is my result String");
            }
        };
        t.run();
        return false;
    }

    @Override
    public String getStatus() {
        //TODO: this should dynamically report what's happening
        return "hardly working or working hard...";
    }

}
