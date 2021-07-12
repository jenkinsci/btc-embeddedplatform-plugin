package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
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

import com.btc.ep.plugins.embeddedplatform.http.EPApiClient;
import com.btc.ep.plugins.embeddedplatform.reporting.ReportService;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

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
        return new BtcWrapUpStepExecution(this, context);
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
class BtcWrapUpStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcWrapUpStep step;

    public BtcWrapUpStepExecution(BtcWrapUpStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
    }

    private ProfilesApi profileApi = new ProfilesApi();
    private ApplicationApi applicationApi = new ApplicationApi();

    @Override
    protected void performAction() throws Exception {
        checkArgument(Configuration.getDefaultApiClient() instanceof EPApiClient,
            "Unexpected Default Api Client");

        /*
         * Generate the project report
         */
        assembleProjectReport();

        /*
         * Save the profile
         */
        String profilePath = step.getProfilePath() == null ? Store.epp.getPath() : step.getProfilePath();
        if (profilePath instanceof String) {
            // save the epp to the designated location
            profileApi.saveProfile(new ProfilePath().path(Store.epp.getPath()));
        }
        /*
         * Exit the application (first softly via API)
         */
        applicationApi.exitApplication(true);
        if (Store.epProcess != null && Store.epProcess.isAlive()) {
            // ... und bist du nicht willig, so brauch ich Gewalt!
            Store.epProcess.destroyForcibly();
        }
        jenkinsConsole.println("Successfully closed BTC EmbeddedPlatform.");
        response = 200;
    }

    private void assembleProjectReport() throws IOException {
        Store.reportData.addSection(Store.testStepSection);
        Store.testStepArgumentSection.setSteps(Store.testStepSection.getSteps());
        Store.reportData.addSection(Store.testStepArgumentSection);
        Store.reportData.addSection(Store.pilInfoSection);
        String endDate = Util.DATE_FORMAT.format(new Date());
        Store.reportData.setEndDate(endDate);
        String durationString = Util.getTimeDiffAsString(new Date(), Store.startDate);
        Store.reportData.setDuration(durationString);
        File report = ReportService.getInstance().generateProjectReport(Store.reportData);
        try {
            ReportService.getInstance().exportReport(report, Store.exportPath);
        } catch (IOException e) {
            throw new IOException("Failed to export project report to " + Store.exportPath + ": " + e.getMessage());
        }
    }

}
