package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.ApplicationApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.ProfilePath;

import com.btc.ep.plugins.embeddedplatform.http.ApiClientThatDoesntSuck;
import com.btc.ep.plugins.embeddedplatform.util.BtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcWrapUpStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;

    @DataBoundConstructor
    public BtcWrapUpStep() {
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
            return "btcWrapUp";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Wrap Up Step";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /**
     * Get profilePath.
     * 
     * @return the profilePath
     */
    public String getProfilePath() {
        return profilePath;

    }

    /**
     * Set profilePath.
     * 
     * @param profilePath the profilePath to set
     */
    @DataBoundSetter
    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcExampleStepExecution extends BtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcWrapUpStep step;

    public BtcExampleStepExecution(BtcWrapUpStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
    }

    private ProfilesApi profileApi = new ProfilesApi();
    private ApplicationApi applicationApi = new ApplicationApi();

    @Override
    protected void performAction() throws Exception {
        String profilePath = step.getProfilePath() == null ? Store.epp.getPath() : step.getProfilePath();
        checkArgument(Configuration.getDefaultApiClient() instanceof ApiClientThatDoesntSuck,
            "Unexpected Default Api Client");
        checkArgument(profilePath instanceof String, "Profile path not available");

        // save the epp to the designated location
        profileApi.saveProfile(new ProfilePath().path(Store.epp.getPath()));
        // exit the application
        applicationApi.exitApplication(true);

        jenkinsConsole.println("Successfully closed BTC EmbeddedPlatform.");
        response = 200;
    }

}
