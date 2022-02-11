package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestExecutionRecordExportInfo;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcExecutionRecordExportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcExecutionRecordExportStep step;

    public BtcExecutionRecordExportStepExecution(BtcExecutionRecordExportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    private ExecutionRecordsApi erApi = new ExecutionRecordsApi();
    private ProfilesApi profilesApi = new ProfilesApi();
    
    @Override
    protected void performAction() throws Exception {
    	// Check preconditions
        try {
            profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
            throw new IllegalStateException("You need an active profile to run tests");
        }
        Path exportDir;
        try {
        	exportDir = resolvePath(step.getDir());
        } catch (Exception e) {
        	log("Error: invalid path: " + step.getDir());
        	log(e.getMessage());
        	failed();
        	return;
        }
        List<String> uids = erApi.getExecutionRecords1()
            .stream()
            .filter(er -> step.getExecutionConfig().equalsIgnoreCase(er.getExecutionConfig())
                && (step.getFolderName() == null || step.getFolderName().equals(er.getFolderName())))
            .map(er -> er.getUid())
            .collect(Collectors.toList());
        if (uids.isEmpty()) {
        	log("Error: no execution records to export found. Did you run any tests yet?");
        	failed();
        	return;
        }
        
        RestExecutionRecordExportInfo data = new RestExecutionRecordExportInfo();
        data.setUiDs(uids);
        data.setExportDirectory(exportDir.toString());
        data.setExportFormat("MDF");
        Job job = erApi.exportExecutionRecords(data);
        Object response = HttpRequester.waitForCompletion(job.getJobID());
        // TODO: the callback is always just null. is there a way of checking the status of the job?

    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcExecutionRecordExportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String dir;
    private String executionConfig;
    private String folderName;

    @DataBoundConstructor
    public BtcExecutionRecordExportStep(String dir, String executionConfig) {
        super();
        if (dir != null) {
            this.dir = dir;
        } else {
            this.dir = Store.exportPath;
        }
        this.executionConfig = executionConfig;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcExecutionRecordExportStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "btcExecutionRecordExport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Execution Record Export Step";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */
    public String getDir() {
        return dir;
    }

    public String getExecutionConfig() {
        return executionConfig;
    }

    public String getFolderName() {
        return folderName;

    }

    @DataBoundSetter
    public void setFolderName(String folderName) {
        this.folderName = folderName;

    }
    /*
     * End of getter/setter section
     */

} // end of step class
