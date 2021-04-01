package com.btc.ep.plugins.embeddedplatform.step;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.TaskListener;

public class BtcStartupStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;
    private String installPath;

    @DataBoundConstructor
    public BtcStartupStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcStartupStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "btcStartup";
        }
    }

    /**
     * Get installPath.
     * 
     * @return the installPath
     */
    public String getInstallPath() {
        return installPath;
    }

    /**
     * Set installPath.
     * 
     * @param installPath the installPath to set
     */
    @DataBoundSetter
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

}
