package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.ExecutionRecordsApi;
import org.openapitools.client.model.ExecutionRecordImportInfo;
import org.openapitools.client.model.Job;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcExecutionRecordImportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcExecutionRecordImportStep step;

    public BtcExecutionRecordImportStepExecution(BtcExecutionRecordImportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    ExecutionRecordsApi erApi = new ExecutionRecordsApi();

    @Override
    protected void performAction() throws Exception {
        Path exportDir = resolvePath(step.getDir());
        File[] files = exportDir.toFile().listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".mdf");

            }
        });
        List<String> paths = new ArrayList<>(files.length);
        for (File file : files) {
            paths.add(file.getPath());
        }
        ExecutionRecordImportInfo data = new ExecutionRecordImportInfo();
        data.setFormat("MDF");
        data.setKind(step.getExecutionConfig());
        data.setPaths(paths);
        data.setFolderName(step.getFolderName());
        Job job = erApi.importExecutionRecord(data);
        HttpRequester.waitForCompletion(job.getJobID()); //response = null

        // print success message and return response code
        jenkinsConsole.println("--> [200] Example step successfully executed.");
        response = 200;
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcExecutionRecordImportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String dir;
    private String executionConfig;
    private String folderName;

    @DataBoundConstructor
    public BtcExecutionRecordImportStep(String dir, String executionConfig) {
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
        return new BtcExecutionRecordImportStepExecution(this, context);
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
