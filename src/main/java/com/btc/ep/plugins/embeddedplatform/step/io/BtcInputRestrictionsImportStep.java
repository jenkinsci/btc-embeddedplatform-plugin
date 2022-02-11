package com.btc.ep.plugins.embeddedplatform.step.io;

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
import org.openapitools.client.api.InputRestrictionsApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.InputRestrictionsFolderObject;

import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcInputRestrictionsImportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String path;

    @DataBoundConstructor
    public BtcInputRestrictionsImportStep(String path) {
        super();
        this.setPath(path);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcInputRestrictionsImportStepExecution(this, context);
    }

    /**
     * Get path.
     * 
     * @return the path
     */
    public String getPath() {
        return path;

    }

    /**
     * Set path.
     * 
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;

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
            return "btcInputRestrictionsImport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Input Restrictions Import Step";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcInputRestrictionsImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcInputRestrictionsImportStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private InputRestrictionsApi inputRestrictionsApi = new InputRestrictionsApi();

    /**
     * Constructor
     *
     * @param step
     * @param context
     */
    public BtcInputRestrictionsImportStepExecution(BtcInputRestrictionsImportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }
    
    private ProfilesApi profilesApi = new ProfilesApi();

    /*
     * Put the desired action here:
     * - checking preconditions
     * - access step parameters (field step: step.getFoo())
     * - calling EP Rest API
     * - print text to the Jenkins console (field: jenkinsConsole)
     * - set response code (field: response)
     */
    @Override
    protected void performAction() throws Exception {
    	// Check preconditions
        try {
            profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
            throw new IllegalStateException("You need an active profile to run tests");
        }
        // Get the path
        Path path = resolvePath(step.getPath());
        checkArgument(path.toFile().exists(), "Error: xml does not exist " + path);

        InputRestrictionsFolderObject file = new InputRestrictionsFolderObject();
        file.setFilePath(path.toString());
        try {
        	inputRestrictionsApi.importFromFile(file);
        } catch (Exception e) {
        	error();
        	log("ERROR on input restrictions import: " + e.getMessage());
        }
        info("Imported input restructions");

    }

}
