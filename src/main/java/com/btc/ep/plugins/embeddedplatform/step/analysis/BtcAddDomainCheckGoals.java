package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.DomainChecksApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.DomainChecksIOInfo;
import org.openapitools.client.model.DomainChecksRangesInput;
import org.openapitools.client.model.DomainChecksRangesInput.ApplyBoundaryChecksEnum;
import org.openapitools.client.model.DomainChecksRangesInput.ApplyInvalidRangesChecksEnum;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestCase;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcAddDomainCheckGoalsStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcAddDomainCheckGoals step;

	public BtcAddDomainCheckGoalsStepExecution(BtcAddDomainCheckGoals step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		AddDomainCheckGoalsStepExecution exec = new AddDomainCheckGoalsStepExecution(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		// exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}
	
class AddDomainCheckGoalsStepExecution extends BtcExecution {
	private static final long serialVersionUID = -4841998758181121586L;
	private BtcAddDomainCheckGoals step;
	private DomainChecksApi domainApi = new DomainChecksApi();
	private ScopesApi scopesApi = new ScopesApi();
	
	public AddDomainCheckGoalsStepExecution(PrintStream logger, StepContext context, Step step) {
		super(logger, context, step);
	}

	@Override
	protected Object performAction() throws Exception {
		// Check preconditions
		int raster;
		try {
			raster = Integer.parseInt(step.getRaster());
		} catch (Exception e) {
			error("Invalid integer value for 'raster': " + step.getRaster() + "");
			return null;
		}
		checkArgument(raster > 0 && raster <= 100, "ERROR: Domain Check Raster must be between 0 and 100: (0, 100]!");

		// check if we have to iterate over all scopes,
		// only top level scope, or the given scope
		if (step.getScopePath() != "*") { // find a specific scope
			List<Scope> scopesList = null;
			try {
				scopesList = scopesApi.getScopesByQuery1(null, TRUE);
			} catch (Exception e) {
				error("Could not query scopes.", e);
				return null;
			}
			checkArgument(!scopesList.isEmpty(), "The profile contains no scopes.");
			String scopeUid = null;
			// get top level scope
			if (step.getScopePath() != null) { // scope given. use it.
				List<Scope> scopes = null;
				try {
					scopes = scopesApi.getScopesByQuery1(step.getScopePath(), FALSE);
				} catch (Exception e) {
					error("Failed to retrieve scope " + step.getScopePath(), e);
					return null;
				}
				checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");
				scopeUid = scopes.get(0).getUid();
			} else { // no scope given. default is top level scope.
				scopeUid = scopesList.get(0).getUid();
			}
			sendPerformAction(scopeUid);
		} else { // else step.getScopePath() == "*". iterate over all scopes
			List<Scope> allScopes = null;
			try {
				allScopes = scopesApi.getScopesByQuery1(null, FALSE);
			} catch (Exception e) {
				error("Failed to retrieve all scopes", e);
				return null;
			}
			checkArgument(!allScopes.isEmpty(), "The profile contains no scopes.");
			for (Scope scope : allScopes) {
				sendPerformAction(scope.getUid());
			}
		}
		result("PASSED");
		info("Finished adding domain checks");
		return null;

	}

	final private void sendPerformAction(String scopeuid) {
		// if we are given a config file, import the XML settings
		// as our domain check goals.
		if (step.getDcXmlPath() != null) {
			String dcXmlPath;
			try {
				dcXmlPath = resolveToString(step.getDcXmlPath());
			} catch (Exception e) {
				log("ERROR: invalid path given: " + step.getDcXmlPath() + ". " + e.getMessage());
				error();
				return;
			}
			DomainChecksIOInfo r = new DomainChecksIOInfo();
			r.setScopeUid(scopeuid);
			r.setFilePath(dcXmlPath);
			try {
				log("Importing Domain Check Goals from XML...");
				Job job = domainApi.importDomainChecksGoals(r);
				HttpRequester.waitForCompletion(job.getJobID(), "result");
				log("Successfully imported domain checks");
			} catch (Exception e) {
				error();
				log("ERROR: Failed to import Domain Checks goals: " + e.getMessage());

			}
		} else { // no config file given-- use our input variables
			// create API object
			DomainChecksRangesInput r = new DomainChecksRangesInput();
			r.setScopeUid(scopeuid);
			r.setApplyBoundaryChecks(step.isActivateBoundaryCheck() ? ApplyBoundaryChecksEnum.TRUE : ApplyBoundaryChecksEnum.FALSE);
			r.setApplyInvalidRangesChecks(step.isActivateRangeViolationCheck() ? ApplyInvalidRangesChecksEnum.TRUE : ApplyInvalidRangesChecksEnum.FALSE);
			r.setPercentage(Integer.parseInt(step.getRaster()));
			try { // send API object
				log("Creating Domain Check Goals...");
				domainApi.createDomainChecksRanges(r);
				log("Successfully created Domain Check Goals");
			} catch (Exception e) {
				error();
				log("Failed to create DomainChecks goals: " + e.getMessage());
			}
		}
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
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

	private static final String PIPELINE_FUNCTION_NAME = "btcaddDomainCheckGoals";
	private static final String JENKINS_DISPLAY_NAME = "BTC Domain Check Goals";

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

		@Override
		public String getFunctionName() {
			return PIPELINE_FUNCTION_NAME;
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return JENKINS_DISPLAY_NAME;
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
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
