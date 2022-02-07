package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

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
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ProgressApi;
import org.openapitools.client.model.CCodeImportInfo;
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
class BtcProfileCreateCStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcProfileCreateCStep step;

    public BtcProfileCreateCStepExecution(BtcProfileCreateCStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    private ProfilesApi profilesApi = new ProfilesApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();

    @Override
    protected void performAction() throws Exception {
        /*
         * Preparation
         */
        Path profilePath = resolvePath(step.getProfilePath());
        Path codeModelPath = resolvePath(step.getCodeModelPath());
        preliminaryChecks(profilePath, codeModelPath);
        Store.epp = profilePath.toFile();
        Store.exportPath = resolvePath(step.getExportPath() != null ? step.getExportPath() : "reports").toString();

        //TODO: Configure ML connection and execute ML Startup Script if needed (requires EP-2535)

        /*
         * Create the profile based on the code model
         */
        profilesApi.createProfile(true);
        Util.setCompilerWithFallback(step.getCompilerShortName(), jenkinsConsole);
        CCodeImportInfo info = new CCodeImportInfo().modelFile(codeModelPath.toString());
        Job job = archApi.importArchitecture(info);
        HttpRequester.waitForCompletion(job.getJobID());
        new ProgressApi().getProgress(job.getJobID());

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
     * Checks if the profilePath and codeModelPath are valid (!= null), discards any loaded
     * profile and warns in case the obsolete option "licenseLocationString" is used.
     *
     * @param profilePath the profile path
     * @param codeModelPath the code model path
     */
    private void preliminaryChecks(Path profilePath, Path codeModelPath) {
        Util.discardLoadedProfileIfPresent(profilesApi);
        if (step.getLicenseLocationString() != null) {
            jenkinsConsole.println(
                "the option 'licenseLocationString' of the btcProfileCreate / btcProfileLoad steps has no effect and will be ignored. Please specify this option with the btcStartup step.");
        }
        checkArgument(profilePath != null, "No valid profile path was provided: " + step.getProfilePath());
        checkArgument(codeModelPath != null, "No valid code model path was provided: " + step.getCodeModelPath());
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcProfileCreateCStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;
    private String exportPath;

    private String codeModelPath;
    private String startupScriptPath;
    private String compilerShortName;
    private String matlabVersion;
    private String matlabInstancePolicy;
    private boolean saveProfileAfterEachStep;
    private String licenseLocationString; // mark as deprecated?

    @DataBoundConstructor
    public BtcProfileCreateCStep(String profilePath, String codeModelPath) {
        super();
        this.profilePath = profilePath;
        this.codeModelPath = codeModelPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcProfileCreateCStepExecution(this, context);
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
            return "btcProfileCreateC";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Profile Creation (C-Code)";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    public String getProfilePath() {
        return profilePath;

    }

    public String getExportPath() {
        return exportPath;

    }

    @DataBoundSetter
    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;

    }

    public String getCodeModelPath() {
        return codeModelPath;
    }

    public String getStartupScriptPath() {
        return startupScriptPath;
    }

    @DataBoundSetter
    public void setStartupScriptPath(String startupScriptPath) {
        this.startupScriptPath = startupScriptPath;
    }

    public String getCompilerShortName() {
        return compilerShortName;
    }

    @DataBoundSetter
    public void setCompilerShortName(String compilerShortName) {
        this.compilerShortName = compilerShortName;
    }

    public String getMatlabVersion() {
        return matlabVersion;
    }

    @DataBoundSetter
    public void setMatlabVersion(String matlabVersion) {
        this.matlabVersion = matlabVersion;
    }

    public String getMatlabInstancePolicy() {
        return matlabInstancePolicy;
    }

    @DataBoundSetter
    public void setMatlabInstancePolicy(String matlabInstancePolicy) {
        this.matlabInstancePolicy = matlabInstancePolicy;
    }

    public boolean isSaveProfileAfterEachStep() {
        return saveProfileAfterEachStep;
    }

    @DataBoundSetter
    public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
        this.saveProfileAfterEachStep = saveProfileAfterEachStep;
    }

    public String getLicenseLocationString() {
        return licenseLocationString;
    }

    @DataBoundSetter
    public void setLicenseLocationString(String licenseLocationString) {
        this.licenseLocationString = licenseLocationString;
    }

    /*
     * End of getter/setter section
     */

} // end of step class
