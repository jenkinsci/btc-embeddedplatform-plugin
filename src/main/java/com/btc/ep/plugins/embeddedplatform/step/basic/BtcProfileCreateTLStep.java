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
import org.openapitools.client.model.Job;
import org.openapitools.client.model.TLImportInfo;
import org.openapitools.client.model.TLImportInfo.CalibrationHandlingEnum;
import org.openapitools.client.model.TLImportInfo.TestModeEnum;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcProfileCreateTLStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcProfileCreateTLStep step;
    private ProfilesApi profilesApi = new ProfilesApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();

    public BtcProfileCreateTLStepExecution(BtcProfileCreateTLStep step, StepContext context) {
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

        /*
         * Create the profile based on the code model
         */
        Path tlModelPath = resolvePath(step.getTlModelPath());
        Path tlScriptPath = resolvePath(step.getTlScriptPath());
        try {
            profilesApi.createProfile(true);
        } catch (Exception e) {
        	log("ERROR. Failed to create profile: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        TLImportInfo info = new TLImportInfo()
            .tlModelFile(tlModelPath.toString())
            .tlInitScript(tlScriptPath.toString())
            .fixedStepSolver(true);
        // Calibration Handling
        CalibrationHandlingEnum calibrationHandling = CalibrationHandlingEnum.EXPLICIT_PARAMETER;
        if (step.getCalibrationHandling().equalsIgnoreCase("LIMITED BLOCKSET")) {
            calibrationHandling = CalibrationHandlingEnum.LIMITED_BLOCKSET;
        } else if (step.getCalibrationHandling().equalsIgnoreCase("OFF")) {
            calibrationHandling = CalibrationHandlingEnum.OFF;
        }
        info.setCalibrationHandling(calibrationHandling);
        // Test Mode (Displays)
        TestModeEnum testMode = TestModeEnum.GREY_BOX;
        if (step.getTestMode().equalsIgnoreCase("BLACK BOX")) {
            testMode = TestModeEnum.BLACK_BOX;
        }
        info.setTestMode(testMode);
        // Legacy Code XML (Environment)
        if (step.getEnvironmentXmlPath() != null) {
            info.setEnvironment(resolvePath(step.getEnvironmentXmlPath()).toString());
        }
        info.setUseExistingCode(step.isReuseExistingCode());
        // TL Subsystem
        if (step.getTlSubsystem() != null) {
            info.setTlSubsystem(step.getTlSubsystem());
        }
        if (step.getSubsystemMatcher() != null) {
        	info.setSubsystemMatcher(step.getSubsystemMatcher());
        }
        if (step.getCalibrationMatcher() != null) {
        	info.setCalibrationMatcher(step.getCalibrationMatcher());
        }
        if (step.getCodeFileMatcher() != null) {
        	info.setCfileMatcher(step.getCodeFileMatcher());
        }
        try {
	        Job job = archApi.importTargetLinkArchitecture1(info);
	        log("Importing TargetLink architecture '" + tlModelPath.toFile().getName() + "'...");
	        HttpRequester.waitForCompletion(job.getJobID());
	    } catch (Exception e) {
        	log("ERROR. Failed to import architecture " + 
        			info.getSlModelFile() + ": " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        /*
         * Wrapping up, reporting, etc.
         */
        String msg = "Successfully created the profile.";
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
public class BtcProfileCreateTLStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String profilePath;
    private String exportPath;

    private String tlModelPath;
    private String tlScriptPath;
    private String tlSubsystem;
    private String environmentXmlPath;
    private String pilConfig;
    private String calibrationHandling = "EXPLICIT PARAM";
    private String testMode = "GREY BOX";
    private boolean reuseExistingCode;
    private String subsystemMatcher;
	private String calibrationMatcher;
    private String codeFileMatcher;
    private String startupScriptPath;
    private String matlabVersion;
    private String matlabInstancePolicy = "AUTO";
    private boolean saveProfileAfterEachStep;
    private String licenseLocationString; // mark as deprecated?

    @DataBoundConstructor
    public BtcProfileCreateTLStep(String profilePath, String tlModelPath) {
        super();
        this.profilePath = profilePath;
        this.tlModelPath = tlModelPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcProfileCreateTLStepExecution(this, context);
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
            return "btcProfileCreateTL";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Profile Creation (Targetlink)";
        }
    }

    public String getProfilePath() {
        return profilePath;
    }

    public String getExportPath() {
        return exportPath;
    }

    public String getTlModelPath() {
        return tlModelPath;
    }

    public String getTlScriptPath() {
        return tlScriptPath;
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

    public String getTlSubsystem() {
        return tlSubsystem;
    }

    public String getEnvironmentXmlPath() {
        return environmentXmlPath;
    }

    public String getPilConfig() {
        return pilConfig;
    }

    public String getCalibrationHandling() {
        return calibrationHandling;
    }

    public String getTestMode() {
        return testMode;
    }

    public boolean isReuseExistingCode() {
        return reuseExistingCode;
    }

    public String getSubsystemMatcher() {
		return subsystemMatcher;
	}

    public String getCalibrationMatcher() {
    	return calibrationMatcher;
    }

    public String getCodeFileMatcher() {
    	return codeFileMatcher;
    }

    @DataBoundSetter
	public void setSubsystemMatcher(String subsystemMatcher) {
		this.subsystemMatcher = subsystemMatcher;
	}

    @DataBoundSetter
	public void setCalibrationMatcher(String calibrationMatcher) {
		this.calibrationMatcher = calibrationMatcher;
	}

    @DataBoundSetter
	public void setCodeFileMatcher(String codeFileMatcher) {
		this.codeFileMatcher = codeFileMatcher;
	}

    @DataBoundSetter
    public void setTlSubsystem(String tlSubsystem) {
        this.tlSubsystem = tlSubsystem;
    }

    @DataBoundSetter
    public void setEnvironmentXmlPath(String environmentXmlPath) {
        this.environmentXmlPath = environmentXmlPath;
    }

    @DataBoundSetter
    public void setPilConfig(String pilConfig) {
        this.pilConfig = pilConfig;
    }

    @DataBoundSetter
    public void setCalibrationHandling(String calibrationHandling) {
        this.calibrationHandling = calibrationHandling;
    }

    @DataBoundSetter
    public void setTestMode(String testMode) {
        this.testMode = testMode;
    }

    @DataBoundSetter
    public void setReuseExistingCode(boolean reuseExistingCode) {
        this.reuseExistingCode = reuseExistingCode;
    }

    @DataBoundSetter
    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    @DataBoundSetter
    public void setTlScriptPath(String tlScriptPath) {
        this.tlScriptPath = tlScriptPath;
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
    public void setSaveProfileAfterEachStep(boolean saveProfileAfterEachStep) {
        this.saveProfileAfterEachStep = saveProfileAfterEachStep;
    }

    @DataBoundSetter
    public void setLicenseLocationString(String licenseLocationString) {
        this.licenseLocationString = licenseLocationString;
    }

    @DataBoundSetter
    public void setMatlabVersion(String matlabVersion) {
        this.matlabVersion = matlabVersion;
    }

    /*
     * End of getter/setter section
     */

} // end of step class
