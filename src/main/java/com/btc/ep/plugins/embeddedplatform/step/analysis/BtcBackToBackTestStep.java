package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.BackToBackTestsApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.GenericResponse;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.google.gson.Gson;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcBackToBackTestStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String reference;
    private String comparison;

    @DataBoundConstructor
    public BtcBackToBackTestStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcExampleStepExecution(this, context);
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
            return "btcBackToBack";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "Perform Back-to-Back Test with BTC EmbeddedPlatform";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /**
     * Get reference.
     * 
     * @return the reference
     */
    public String getReference() {
        return reference;

    }

    /**
     * Set reference.
     * 
     * @param reference the reference to set
     */
    @DataBoundSetter
    public void setReference(String reference) {
        this.reference = reference;

    }

    /**
     * Get comparison.
     * 
     * @return the comparison
     */
    public String getComparison() {
        return comparison;

    }

    /**
     * Set comparison.
     * 
     * @param comparison the comparison to set
     */
    @DataBoundSetter
    public void setComparison(String comparison) {
        this.comparison = comparison;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcExampleStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;

    private BtcBackToBackTestStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private String status;

    /**
     * Constructor
     *
     * @param btcStartupStep
     * @param context
     */
    public BtcExampleStepExecution(BtcBackToBackTestStep btcStartupStep, StepContext context) {
        super(context);
        this.step = btcStartupStep;
    }

    /*
     * The start method must either
     * - start an asychronous process in its own thread (e.g. TimerTask) and then return false or
     * - perform the desired action immediately (shouldn't take more that 1-2 seconds) and then return true
     */
    @Override
    public boolean start() throws Exception {
        /*
         * We can use something like this timer task to implement the desired action, i.e.:
         * - process the step parameters
         * - check preconditions
         * -> is the API already connected?
         * -> is EP in the expected state (e.g. profile loaded, architecture imported, test cases available...)?
         * -> do all referenced files exist?
         * - perform action using EP SDK
         */
        TimerTask t = new TimerTask() {

            private BackToBackTestsApi b2bApi = new BackToBackTestsApi();
            private ScopesApi scopesApi = new ScopesApi();;
            private ProfilesApi profilesApi = new ProfilesApi();

            @Override
            public void run() {
                // This is what's actually executed (currently just prints some text to the Jenkins console):
                try {
                    PrintStream jenkinsConsole = getContext().get(TaskListener.class).getLogger();

                    // Check preconditions
                    try {
                        profilesApi.getCurrentProfile(); // throws Exception if no profile is active
                    } catch (Exception e) {
                        throw new IllegalStateException("You need an active profile to perform a Back-to-Back Test");
                    }
                    List<Scope> scopes = scopesApi.getScopesByQuery1(null, true);
                    checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");

                    // Prepare data for B2B test
                    Scope toplevelScope = scopes.get(0);
                    BackToBackTestExecutionData data = new BackToBackTestExecutionData();
                    data.setRefMode(step.getReference());
                    data.setCompMode(step.getComparison());

                    // Execute B2B test and return result
                    Job job = b2bApi.executeBackToBackTestOnScope(toplevelScope.getUid(), data);
                    String b2bTestUid = HttpRequester.waitForCompletion(job.getJobID());
                    // Workaround for EP-2401 / EP-2355
                    // BackToBackTest b2bTest = b2bApi.getTestByUID(b2bTestUid);
                    GenericResponse response = HttpRequester.get("/ep/b2b/" + b2bTestUid);
                    String json = response.getContent().replace("PASSED", "Passed").replace("FAILED", "Failed");
                    @SuppressWarnings ("unchecked")
                    Map<String, Object> genericMap = new Gson().fromJson(json, HashMap.class);
                    String verdictStatus = (String)genericMap.get("verdictStatus");
                    // End of workaround
                    jenkinsConsole.println("Back-to-Back Test finished with result: " + verdictStatus);
                    getContext().onSuccess(verdictStatus);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    getContext().onFailure(e1);
                }
                // Important: always call this method (unless onFailure is called), otherwise Jenkins will wait forever
            }
        };
        // trigger the desired action by calling the run() method
        t.run();
        // return false (see explanation in comment on start() method)
        return false;
    }

    @Override
    public String getStatus() {
        return status;
    }

}
