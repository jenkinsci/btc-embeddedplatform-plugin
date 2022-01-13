package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.Serializable;
import java.nio.file.Path;
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
import org.openapitools.client.model.ECImportInfo;
import org.openapitools.client.model.ECImportInfo.ParameterHandlingEnum;
import org.openapitools.client.model.ECImportInfo.TestModeEnum;
import org.openapitools.client.model.Job;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileCreateECStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcProfileCreateECStep step;
    private ProfilesApi profilesApi = new ProfilesApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();

    public BtcProfileCreateECStepExecution(BtcProfileCreateECStep step, StepContext context) {
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
        Path profilePath = resolvePath(step.getProfilePath());
        preliminaryChecks();
        Store.epp = profilePath.toFile();
        Store.exportPath = resolvePath(step.getExportPath() != null ? step.getExportPath() : "reports").toString();

        /*
         * Prepare Matlab
         */
        Util.configureMatlabConnection(step.getMatlabVersion(), step.getMatlabInstancePolicy());

        //TODO: Execute Startup Script (requires EP-2535)

        //TODO: Create Wrapper Model (requires EP-2538)

        /*
         * Create the profile based on the code model
         */
        Path slModelPath = resolvePath(step.getSlModelPath());
        Path slScriptPath = resolvePath(step.getSlScriptPath());
        profilesApi.createProfile();
        ECImportInfo info = new ECImportInfo()
            .ecModelFile(slModelPath.toString())
            .ecInitScript(slScriptPath.toString())
            .fixedStepSolver(true);
        //TODO: support two-step import process that allows us to filter subsystems/calibrations/codefiles (requires EP-2569)
        configureParameterHandling(info, step.getParameterHandling());
        configureTestMode(info, step.getTestMode());
        Job job = archApi.importEmbeddedCoderArchitecture(info);
        HttpRequester.waitForCompletion(job.getJobID());

        /*
         * Wrapping up, reporting, etc.
         */
        String msg = "Successfully created the profile.";
        detailWithLink(Store.epp.getName(), profilePath.toString());
        response = 200;
        jenkinsConsole.println(msg);
        info(msg);
    }

    /**
     * Derives the enumValue from the given string and sets it on the ECImportInfo object.
     *
     * @param info the ECImportInfo object
     * @param parameterHandling the string from the user to pick the right enum
     * @throws ApiException in case of invalid enum string
     */
    private void configureParameterHandling(ECImportInfo info, String parameterHandling) throws ApiException {
        try {
            ParameterHandlingEnum valueAsEnum = ParameterHandlingEnum.fromValue(parameterHandling.toUpperCase());
            info.setParameterHandling(valueAsEnum);
        } catch (Exception e) {
            throw new ApiException("The specifide parameterHandling enum is not valid. Possible values are: "
                + ParameterHandlingEnum.values());
        }
    }

    /**
     * Derives the enumValue from the given string and sets it on the ECImportInfo object.
     *
     * @param info the ECImportInfo object
     * @param testMode the string from the user to pick the right enum
     * @throws ApiException in case of invalid enum string
     */
    private void configureTestMode(ECImportInfo info, String testMode) throws ApiException {
        try {
            TestModeEnum valueAsEnum = TestModeEnum.fromValue(testMode.toUpperCase());
            info.setTestMode(valueAsEnum);
        } catch (Exception e) {
            throw new ApiException("The specifide testMode enum is not valid. Possible values are: "
                + TestModeEnum.values());
        }
    }

    /**
     * Discards any loaded profile and warns in case the obsolete option "licenseLocationString" is used.
     *
     * @param profilePath the profile path
     * @param slModelPath the slModelPath
     * @param addInfoModelPath
     */
    private void preliminaryChecks() {
        Util.discardLoadedProfileIfPresent(profilesApi);
        if (step.getLicenseLocationString() != null) {
            jenkinsConsole.println(
                "the option 'licenseLocationString' of the btcProfileCreate / btcProfileLoad steps has no effect and will be ignored. Please specify this option with the btcStartup step.");
        }
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcProfileCreateECStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;
    private String exportPath;

    private String slModelPath;
    private String slScriptPath;
    private String parameterHandling = ParameterHandlingEnum.EXPLICIT_PARAMETER.toString();
    private String testMode = TestModeEnum.GREY_BOX.toString();
    private String startupScriptPath;
    private String matlabVersion;
    private String matlabInstancePolicy = "AUTO";
    private boolean saveProfileAfterEachStep;
    private String licenseLocationString; // mark as deprecated?

    @DataBoundConstructor
    public BtcProfileCreateECStep(String profilePath, String slModelPath) {
        super();
        this.profilePath = profilePath;
        this.slModelPath = slModelPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcProfileCreateECStepExecution(this, context);
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
            return "btcProfileCreateEC";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Profile Creation (EmbeddedCoder)";
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

    public String getParameterHandling() {
        return parameterHandling;

    }

    public String getTestMode() {
        return testMode;

    }

    @DataBoundSetter
    public void setTestMode(String testMode) {
        this.testMode = testMode;

    }

    @DataBoundSetter
    public void setParameterHandling(String parameterHandling) {
        this.parameterHandling = parameterHandling;

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
