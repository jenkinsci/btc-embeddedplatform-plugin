package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.Serializable;
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
import org.openapitools.client.ApiException;
import org.openapitools.client.api.BackToBackTestsApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Job;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcSimulationStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcSimulationStep step;

    public BtcSimulationStepExecution(BtcSimulationStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    private ProfilesApi profileApi = new ProfilesApi();
    private BackToBackTestsApi b2bApi = new BackToBackTestsApi(); // workaround because there's no pure simulation api
    private ScopesApi scopeApi = new ScopesApi();

    @Override
    protected void performAction() throws Exception {
        // Check preconditions
        try {
            profileApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
            throw new IllegalStateException("You need an active profile to run tests");
        }

        // Prepare data
        List<String> executionConfigNames = Util.getValuesFromCsv(step.getExecutionConfigString());
        //TODO: query all Execution configs if nothing is specified (requires EP-2536)

        /*
         * Workaround because there's no pure simulation api
         */
        List<String> scopeUids = null;
        try {
            scopeUids = scopeApi.getScopesByQuery1(null, false).stream().map(scope -> scope.getUid()).collect(Collectors.toList());
        } catch (Exception e) {
        	log("ERROR. could not get any scopes: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	error();
        }
        for (String executionConfig : executionConfigNames) {
            BackToBackTestExecutionData data = new BackToBackTestExecutionData();
            data.refMode(executionConfig);
            data.compMode(executionConfig);
            for (String scopeUid : scopeUids) {
                try {
                    Job job = b2bApi.executeBackToBackTestOnScope(scopeUid, data);
                    HttpRequester.waitForCompletion(job.getJobID());
                } catch (ApiException e) {
                    // see EP-2568
                	log("WARNING. failed to execute for scope " + scopeUid + ": " + e.getMessage());
                	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
                	warning();
                }
            }
        }
        /*
         * End of workaround
         */

        // print success message and return response code
        //log("--> [200] Simulation successfully executed.");
        response = 200;
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcSimulationStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String executionConfigString;

    @DataBoundConstructor
    public BtcSimulationStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcSimulationStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "btcSimulation";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Simulation Step";
        }
    }

    public String getExecutionConfigString() {
        return executionConfigString;
    }

    @DataBoundSetter
    public void setExecutionConfigString(String executionConfigString) {
        this.executionConfigString = executionConfigString;
    }

    /*
     * End of getter/setter section
     */

} // end of step class
