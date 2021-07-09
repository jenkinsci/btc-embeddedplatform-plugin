package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.BackToBackTestsApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.BackToBackTest.VerdictStatusEnum;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.GenericResponse;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.util.BtcStepExecution;
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
        return new BtcBackToBackExecution(this, context);
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
class BtcBackToBackExecution extends BtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcBackToBackTestStep step;

    public BtcBackToBackExecution(BtcBackToBackTestStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
    }

    private BackToBackTestsApi b2bApi = new BackToBackTestsApi();
    private ScopesApi scopesApi = new ScopesApi();
    private ProfilesApi profilesApi = new ProfilesApi();

    @Override
    protected void performAction() throws Exception {
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
        GenericResponse httpResponse = HttpRequester.get("/ep/b2b/" + b2bTestUid);
        String json = httpResponse.getContent().replace("PASSED", "Passed").replace("FAILED", "Failed");
        @SuppressWarnings ("unchecked")
        Map<String, Object> genericMap = new Gson().fromJson(json, HashMap.class);
        String verdictStatus = (String)genericMap.get("verdictStatus");
        // End of workaround
        jenkinsConsole.println("Back-to-Back Test finished with result: " + verdictStatus);

        if (VerdictStatusEnum.PASSED.name().equalsIgnoreCase(verdictStatus)) {
            response = 200;
        } else if (VerdictStatusEnum.FAILED_ACCEPTED.name().equalsIgnoreCase(verdictStatus)) {
            response = 201;
        } else if (VerdictStatusEnum.FAILED.name().equalsIgnoreCase(verdictStatus)) {
            response = 300;
        } else if (VerdictStatusEnum.ERROR.name().equalsIgnoreCase(verdictStatus)) {
            response = 400;
        } else {
            response = 500;
        }
    }

}
