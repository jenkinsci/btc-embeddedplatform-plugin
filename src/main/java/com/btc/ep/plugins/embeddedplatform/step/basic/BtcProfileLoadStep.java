package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.openapitools.client.model.Job;
import org.openapitools.client.model.ProfilePath;
import org.openapitools.client.model.UpdateModelPath;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.util.BtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;

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
public class BtcProfileLoadStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;
    private String exportPath;
    private boolean updateRequired;

    private String tlModelPath;
    private String tlScriptPath;
    private String environmentXmlPath;
    private String slModelPath;
    private String slScriptPath;
    private String addInfoModelPath;
    private String codeModelPath;
    private String startupScriptPath;
    private String compilerShortName;
    private String pilConfig;
    private int pilTimetout;
    private String matlabVersion;
    private String matlabInstancePolicy;
    private boolean saveProfileAfterEachStep;
    private String licenseLocationString; // mark as deprecated?

    private boolean showProgress;

    @DataBoundConstructor
    public BtcProfileLoadStep(String profilePath) {
        super();
        this.profilePath = profilePath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcProfileLoadStepExecution(this, context);
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
            return "btcProfileLoad";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Profile Load Step";
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
     * Get updateRequired.
     * 
     * @return the updateRequired
     */
    public boolean isUpdateRequired() {
        return updateRequired;

    }

    /**
     * Set updateRequired.
     * 
     * @param updateRequired the updateRequired to set
     */
    @DataBoundSetter
    public void setUpdateRequired(boolean updateRequired) {
        this.updateRequired = updateRequired;
    }

    /**
     * Get exportPath.
     * 
     * @return the exportPath
     */
    public String getExportPath() {
        return exportPath;

    }

    /**
     * Set exportPath.
     * 
     * @param exportPath the exportPath to set
     */
    @DataBoundSetter
    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;

    }

    /**
     * Get tlModelPath.
     * 
     * @return the tlModelPath
     */
    public String getTlModelPath() {
        return tlModelPath;
    }

    /**
     * Set tlModelPath.
     * 
     * @param tlModelPath the tlModelPath to set
     */
    @DataBoundSetter
    public void setTlModelPath(String tlModelPath) {
        this.tlModelPath = tlModelPath;
    }

    /**
     * Get tlScriptPath.
     * 
     * @return the tlScriptPath
     */
    public String getTlScriptPath() {
        return tlScriptPath;
    }

    /**
     * Set tlScriptPath.
     * 
     * @param tlScriptPath the tlScriptPath to set
     */
    @DataBoundSetter
    public void setTlScriptPath(String tlScriptPath) {
        this.tlScriptPath = tlScriptPath;
    }

    /**
     * Get environmentXmlPath.
     * 
     * @return the environmentXmlPath
     */
    public String getEnvironmentXmlPath() {
        return environmentXmlPath;
    }

    /**
     * Set environmentXmlPath.
     * 
     * @param environmentXmlPath the environmentXmlPath to set
     */
    @DataBoundSetter
    public void setEnvironmentXmlPath(String environmentXmlPath) {
        this.environmentXmlPath = environmentXmlPath;
    }

    /**
     * Get slModelPath.
     * 
     * @return the slModelPath
     */
    public String getSlModelPath() {
        return slModelPath;
    }

    /**
     * Set slModelPath.
     * 
     * @param slModelPath the slModelPath to set
     */
    @DataBoundSetter
    public void setSlModelPath(String slModelPath) {
        this.slModelPath = slModelPath;
    }

    /**
     * Get slScriptPath.
     * 
     * @return the slScriptPath
     */
    public String getSlScriptPath() {
        return slScriptPath;
    }

    /**
     * Set slScriptPath.
     * 
     * @param slScriptPath the slScriptPath to set
     */
    @DataBoundSetter
    public void setSlScriptPath(String slScriptPath) {
        this.slScriptPath = slScriptPath;
    }

    /**
     * Get addInfoModelPath.
     * 
     * @return the addInfoModelPath
     */
    public String getAddInfoModelPath() {
        return addInfoModelPath;
    }

    /**
     * Set addInfoModelPath.
     * 
     * @param addInfoModelPath the addInfoModelPath to set
     */
    @DataBoundSetter
    public void setAddInfoModelPath(String addInfoModelPath) {
        this.addInfoModelPath = addInfoModelPath;
    }

    /**
     * Get codeModelPath.
     * 
     * @return the codeModelPath
     */
    public String getCodeModelPath() {
        return codeModelPath;
    }

    /**
     * Set codeModelPath.
     * 
     * @param codeModelPath the codeModelPath to set
     */
    @DataBoundSetter
    public void setCodeModelPath(String codeModelPath) {
        this.codeModelPath = codeModelPath;
    }

    /**
     * Get startupScriptPath.
     * 
     * @return the startupScriptPath
     */
    public String getStartupScriptPath() {
        return startupScriptPath;
    }

    /**
     * Set startupScriptPath.
     * 
     * @param startupScriptPath the startupScriptPath to set
     */
    @DataBoundSetter
    public void setStartupScriptPath(String startupScriptPath) {
        this.startupScriptPath = startupScriptPath;
    }

    /**
     * Get compilerShortName.
     * 
     * @return the compilerShortName
     */
    public String getCompilerShortName() {
        return compilerShortName;
    }

    /**
     * Set compilerShortName.
     * 
     * @param compilerShortName the compilerShortName to set
     */
    @DataBoundSetter
    public void setCompilerShortName(String compilerShortName) {
        this.compilerShortName = compilerShortName;
    }

    /**
     * Get pilConfig.
     * 
     * @return the pilConfig
     */
    public String getPilConfig() {
        return pilConfig;
    }

    /**
     * Set pilConfig.
     * 
     * @param pilConfig the pilConfig to set
     */
    @DataBoundSetter
    public void setPilConfig(String pilConfig) {
        this.pilConfig = pilConfig;
    }

    /**
     * Get pilTimetout.
     * 
     * @return the pilTimetout
     */
    public int getPilTimetout() {
        return pilTimetout;
    }

    /**
     * Set pilTimetout.
     * 
     * @param pilTimetout the pilTimetout to set
     */
    @DataBoundSetter
    public void setPilTimetout(int pilTimetout) {
        this.pilTimetout = pilTimetout;
    }

    /**
     * Get matlabVersion.
     * 
     * @return the matlabVersion
     */
    public String getMatlabVersion() {
        return matlabVersion;
    }

    /**
     * Set matlabVersion.
     * 
     * @param matlabVersion the matlabVersion to set
     */
    @DataBoundSetter
    public void setMatlabVersion(String matlabVersion) {
        this.matlabVersion = matlabVersion;
    }

    /**
     * Get matlabInstancePolicy.
     * 
     * @return the matlabInstancePolicy
     */
    public String getMatlabInstancePolicy() {
        return matlabInstancePolicy;
    }

    /**
     * Set matlabInstancePolicy.
     * 
     * @param matlabInstancePolicy the matlabInstancePolicy to set
     */
    @DataBoundSetter
    public void setMatlabInstancePolicy(String matlabInstancePolicy) {
        this.matlabInstancePolicy = matlabInstancePolicy;
    }

    /**
     * Get saveProfileAfterEachStep.
     * 
     * @return the saveProfileAfterEachStep
     */
    public boolean isSaveProfileAfterEachStep() {
        return saveProfileAfterEachStep;
    }

    /**
     * Set saveProfileAfterEachStep.
     * 
     * @param saveProfileAfterEachStep the saveProfileAfterEachStep to set
     */
    @DataBoundSetter
    public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
        this.saveProfileAfterEachStep = saveProfileAfterEachStep;
    }

    /**
     * Get licenseLocationString.
     * 
     * @return the licenseLocationString
     */
    public String getLicenseLocationString() {
        return licenseLocationString;
    }

    /**
     * Set licenseLocationString.
     * 
     * @param licenseLocationString the licenseLocationString to set
     */
    @DataBoundSetter
    public void setLicenseLocationString(String licenseLocationString) {
        this.licenseLocationString = licenseLocationString;
    }

    /**
     * Get showProgress.
     * 
     * @return the showProgress
     */
    public boolean isShowProgress() {
        return showProgress;

    }

    /**
     * Set showProgress.
     * 
     * @param showProgress the showProgress to set
     */
    @DataBoundSetter
    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileLoadStepExecution extends BtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcProfileLoadStep step;

    public BtcProfileLoadStepExecution(BtcProfileLoadStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
    }

    private ProfilesApi profilesApi = new ProfilesApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();

    @Override
    protected void performAction() throws Exception {
        /*
         * Preliminary checks
         */
        discardLoadedProfileIfPresent();
        Path profilePath = resolvePath(step.getProfilePath());
        checkArgument(profilePath != null, "No valid profile path was provided: " + step.getProfilePath());
        checkArgument(profilePath.toFile().exists(),
            "Error: Profile does not exist! " + step.getProfilePath());
        Store.epp = profilePath.toFile();
        /*
         * Load the profile
         */
        profilesApi.openProfile(step.getProfilePath());
        updateModelPaths();
        String msg = "Successfully loaded the profile";
        response = 200;
        /*
         * Update architecture if required
         */
        if (step.isUpdateRequired()) {
            Job archUpdate = archApi.architectureUpdate();
            HttpRequester.waitForCompletion(archUpdate.getJobID());
            msg += " and updated the architecture";
            response = 201;
        }
        jenkinsConsole.println(msg + ".");
    }

    /**
     * Checks if the user passed any model paths. If that's the case this method updates them in the profile.
     * 
     * @throws Exception
     */
    private void updateModelPaths() throws Exception {
        UpdateModelPath updateModelPath = new UpdateModelPath();
        // resolve paths from pipeline
        Path p;
        p = resolvePath(step.getTlModelPath());
        if (p != null) {
            updateModelPath.setTlModelFile(p.toFile().getCanonicalPath());
        }
        p = resolvePath(step.getTlScriptPath());
        if (p != null) {
            updateModelPath.setTlInitScript(p.toFile().getCanonicalPath());
        }
        p = resolvePath(step.getSlModelPath());
        if (p != null) {
            updateModelPath.setSlModelFile(p.toFile().getCanonicalPath());
        }
        p = resolvePath(step.getSlScriptPath());
        if (p != null) {
            updateModelPath.setSlInitScript(p.toFile().getCanonicalPath());
        }
        //TODO: continue

        archApi.updateModelPaths("", updateModelPath);

    }

    /**
     * Converts an absolute or relative path into a Path object.
     *
     * @param filePathString An absolute or relative path (relative to the pipelines pwd)
     * @return the path object
     */
    private Path resolvePath(String filePathString) {
        if (filePathString != null) {
            try {
                Path path = Paths.get(filePathString);
                if (path.isAbsolute()) {
                    return path;
                } else {
                    FilePath workspace = getContext().get(FilePath.class);
                    String baseDir = Paths.get(workspace.toURI()).toString();
                    return new File(baseDir, path.toString()).toPath();
                }
            } catch (Exception e) {
                System.out.println("Cannot resolve path from " + filePathString);
            }
        }
        return null;

    }

    private void discardLoadedProfileIfPresent() {
        try {
            Path tmpFile = Files.createTempFile("deleteme", ".epp");
            profilesApi.saveProfile(new ProfilePath().path(tmpFile.toString()));
            Files.delete(tmpFile);
        } catch (Exception e) {
            //ignored
        }
    }

}
