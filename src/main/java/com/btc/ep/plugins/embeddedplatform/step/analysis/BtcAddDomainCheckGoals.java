package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.DomainChecksApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RestDomainChecksIOInfo;
import org.openapitools.client.model.RestDomainChecksRangesInput;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Status;

import hudson.Extension;
import hudson.model.TaskListener;

/*
 * ################################################################################################
 * #                                                                                              #
 * #  THIS IS A TEMPLATE: YOU MAY COPY THIS FILE AS A STARTING POINT TO IMPLEMENT FURTHER STEPS.  #
 * #                                                                                              # 
 * ################################################################################################
 */

/**
 * This class defines what happens when the above step is executed
 */
class BtcAddDomainCheckGoalsStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcAddDomainCheckGoals step;

    public BtcAddDomainCheckGoalsStepExecution(BtcAddDomainCheckGoals step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    /*
     * Put the desired action here:
     * - checking preconditions
     * - access step parameters (field step: step.getFoo())
     * - calling EP Rest API
     * - print text to the Jenkins console (field: jenkinsConsole)
     * - set response code (field: response)
     */
    private DomainChecksApi domainApi = new DomainChecksApi();
    private ScopesApi scopesApi = new ScopesApi();
    private ProfilesApi profilesApi = new ProfilesApi();
    
    @Override
    protected void performAction() throws Exception {
  
    	// Check preconditions
        try {
            profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
        	log("ERROR: No active profile found. Did you successfuly load one in?");
        	error();
            throw new IllegalStateException("You need an active profile for the current command");
        }
        List<Scope> scopesList = null;
        try {
        	scopesList = scopesApi.getScopesByQuery1(null, true);
        } catch(Exception e) {
        	log("ERROR could not query scopes: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        }
        checkArgument(!scopesList.isEmpty(), "The profile contains no scopes.");
        int rast;
        try {
    		rast = Integer.parseInt(step.getRaster());
    	} catch (Exception e) {
    		log("WARNING: invalid integer '" + step.getRaster() + "'. Changing to default value of 25 instead. This will affect results!");
    		warning();
    		rast = 25;
    		step.setRaster("25");
    	}
        checkArgument(rast > 0 && rast <= 100,
        		"ERROR: Domain Check Raster must be between 0 and 100: (0, 100]!");
        
    	// check if we have to iterate over all scopes,
    	// only top level scope, or the given scope
    	if (step.getScopePath() != "*") { // find a specific scope
    		String scope = null;
    		// get top level scope
    		if (step.getScopePath() != null) { // scope given. use it.
    			List<Scope> scopes = null;
    			try {
    				scopes = scopesApi.getScopesByQuery1(step.getScopePath(), false);
    			} catch (Exception e) {
    				log("ERROR getting scope " + step.getScopePath() + ": "+ e.getMessage());
    				try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
    			}
    			checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");
    			scope = scopes.get(0).getUid();
    		} else { // no scope given. default is top level scope.
        		List<Scope> scopes = null;
        		try {
        			scopes = scopesApi.getScopesByQuery1(null, true);
        		} catch (Exception e) {
    				log("ERROR getting top level scope: " + e.getMessage());
    				try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
    			}
        		checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");
                scope = scopes.get(0).getUid();
        	}
    		SendPerformAction(scope);
    	} else { // else step.getScopePath() == "*". iterate over all scopes
    		List<Scope> scopes = null;
    		try {
    			scopes = scopesApi.getScopesByQuery1(null, false);
    		} catch (Exception e) {
				log("ERROR getting list of all scopes: " + e.getMessage());
				try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
			}
    		checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");
    		for(Scope scope: scopes) {
    			SendPerformAction(scope.getUid());
    		}
    	}
    	result("PASSED");
    	info("Finished adding domain checks");
    	
    }
    final private void SendPerformAction(String scopeuid) {
    	// if we are given a config file, import the XML settings
    	// as our domain check goals.
    	if (step.getDcXmlPath() != null) {
    		Path DcXmlPath;
    		try {
    			DcXmlPath = resolvePath(step.getDcXmlPath());
    		} catch (Exception e) {
    			log("ERROR: invalid path given: "+step.getDcXmlPath() + ". " + e.getMessage());
    			error();
    			return;
    		}
        	RestDomainChecksIOInfo r = new RestDomainChecksIOInfo();
        	r.setScopeUid(scopeuid);
        	r.setFilePath(DcXmlPath.toString());
        	try {
        		Job job = domainApi.importDomainChecksGoals(r);
        		HttpRequester.waitForCompletion(job.getJobID(), "result");
	        	log("Successfully imported domain checks for scope " + scopeuid + ": " + response);
			} catch (Exception e) {
				error();
				log("ERROR: failed DomainChecks API call for UID " + scopeuid + ": " + e.getMessage());
				
			}	
        } else { // no config file given-- use our input variables
    		// create API object
        	RestDomainChecksRangesInput r = new RestDomainChecksRangesInput();
    		r.setScopeUid(scopeuid);
    		r.setApplyBoundaryChecks(step.isActivateBoundaryCheck());
    		r.setApplyInvalidRangesChecks(step.isActivateRangeViolationCheck());
    		r.setPercentage(Integer.parseInt(step.getRaster()));
    		try { //send API object
    			String response = domainApi.createDomainChecksRanges(r);
    			log("Successfully updated domain checks for scope " + scopeuid + ": " + response);
    		} catch (Exception e) {
    			error();
    			log("failed DomainChecks API call for UID " + scopeuid + ": " + e.getMessage());
    		}
        }
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcAddDomainCheckGoals extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String scopePath;
    private String dcXmlPath;
    private String raster = "25";
    private boolean activateRangeViolationCheck = false;
    private boolean activateBoundaryCheck = false;

    @DataBoundConstructor
    public BtcAddDomainCheckGoals() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcAddDomainCheckGoalsStepExecution(this, context);
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
            return "btcaddDomainCheckGoals";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Add Domain Check Goals";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

	public String getScopePath() {
		return scopePath;
	}

    @DataBoundSetter
	public void setScopePath(String scopePath) {
		this.scopePath = scopePath;
	}

	public String getDcXmlPath() {
		return dcXmlPath;
	}

    @DataBoundSetter
	public void setDcXmlPath(String dcXmlPath) {
		this.dcXmlPath = dcXmlPath;
	}

	public String getRaster() {
		return raster;
	}

    @DataBoundSetter
	public void setRaster(String raster) {
		this.raster = raster;
	}

	public boolean isActivateRangeViolationCheck() {
		return activateRangeViolationCheck;
	}

    @DataBoundSetter
	public void setActivateRangeViolationCheck(boolean activateRangeViolationCheck) {
		this.activateRangeViolationCheck = activateRangeViolationCheck;
	}

	public boolean isActivateBoundaryCheck() {
		return activateBoundaryCheck;
	}

    @DataBoundSetter
	public void setActivateBoundaryCheck(boolean activateBoundaryCheck) {
		this.activateBoundaryCheck = activateBoundaryCheck;
	}

    /*
     * End of getter/setter section
     */

} // end of step class
