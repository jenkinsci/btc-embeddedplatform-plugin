package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.SLImportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileCreateSLStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcProfileCreateSLStep step;
    private ProfilesApi profilesApi = new ProfilesApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();

    public BtcProfileCreateSLStepExecution(BtcProfileCreateSLStep step, StepContext context) {
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
        /*
         * Preparation
         */
        String profilePath = getProfilePathOrDefault(step.getProfilePath());
        String slModelPath = toRemoteAbsolutePathString(step.getSlModelPath());
        String slScriptPath = toRemoteAbsolutePathString(step.getSlScriptPath());
        preliminaryChecks();
        Store.epp = resolveInAgentWorkspace(profilePath);
        Store.exportPath = toRemoteAbsolutePathString(step.getExportPath() != null ? step.getExportPath() : "reports").toString();

        /*
         * Prepare Matlab
         */
        Util.configureMatlabConnection(step.getMatlabVersion(), step.getMatlabInstancePolicy());

        //TODO: Execute Startup Script (requires EP-2535)
        try {
        	profilesApi.createProfile(true);
        } catch (Exception e) {
        	log("ERROR. Failed to create profile: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        SLImportInfo info = new SLImportInfo()
            .slModelFile(slModelPath.toString())
            .slInitScriptFile(slScriptPath.toString());
        try {
	        Job job = archApi.importSimulinkArchitecture(info);
	        log("Importing Simulink architecture...");
	        HttpRequester.waitForCompletion(job.getJobID());
        } catch (Exception e) {
        	log("ERROR: Failed to import architecture: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        /*
         * Wrapping up, reporting, etc.
         */
        String msg = "Architecture Import successful.";
        detailWithLink(Store.epp.getName(), profilePath.toString());
        response = 200;
        log(msg);
        info(msg);
    }

    /**
     * Discards any loaded profile and warns in case the obsolete option "licenseLocationString" is used.
     *
     * @param profilePath the profile path
     * @param slModelPath the slModelPath
     * @param addInfoModelPath
     */
    private void preliminaryChecks() {
        if (step.getLicenseLocationString() != null) {
            log(
                "the option 'licenseLocationString' of the btcProfileCreate / btcProfileLoad steps has no effect and will be ignored. Please specify this option with the btcStartup step.");
        }
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcProfileCreateSLStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;
    private String exportPath;

    private String slModelPath;
    private String slScriptPath;
    private String startupScriptPath;
    private String matlabVersion;
    private String matlabInstancePolicy = "AUTO";
    private boolean saveProfileAfterEachStep;
    private String licenseLocationString; // mark as deprecated?

    @DataBoundConstructor
    public BtcProfileCreateSLStep(String profilePath, String slModelPath, String addInfoModelPath) {
        super();
        this.profilePath = profilePath;
        this.slModelPath = slModelPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcProfileCreateSLStepExecution(this, context);
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
            return "btcProfileCreateSL";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Profile Creation (Simulink)";
        }
    }

    public String getProfilePath() {
        return profilePath;
    }

    public String getExportPath() {
        return exportPath;
    }

    public String getSlModelPath() {
        return slModelPath;
    }

    public String getSlScriptPath() {
        return slScriptPath;
    }


    public String getStartupScriptPath() {
        return startupScriptPath;
    }

    public String getMatlabVersion() {
        return matlabVersion;
    }

    public String getMatlabInstancePolicy() {
        return matlabInstancePolicy;
    }

    public boolean isSaveProfileAfterEachStep() {
        return saveProfileAfterEachStep;
    }

    public String getLicenseLocationString() {
        return licenseLocationString;
    }

    @DataBoundSetter
    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    @DataBoundSetter
    public void setSlScriptPath(String slScriptPath) {
        this.slScriptPath = slScriptPath;
    }

    @DataBoundSetter
    public void setStartupScriptPath(String startupScriptPath) {
        this.startupScriptPath = startupScriptPath;
    }

    @DataBoundSetter
    public void setMatlabInstancePolicy(String matlabInstancePolicy) {
        this.matlabInstancePolicy = matlabInstancePolicy;
    }

    @DataBoundSetter
    public void setMatlabVersion(String matlabVersion) {
        this.matlabVersion = matlabVersion;
    }

    @DataBoundSetter
    public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
        this.saveProfileAfterEachStep = saveProfileAfterEachStep;
    }

    @DataBoundSetter
    public void setLicenseLocationString(String licenseLocationString) {
        this.licenseLocationString = licenseLocationString;
    }

    /*
     * End of getter/setter section
     */

} // end of step class
