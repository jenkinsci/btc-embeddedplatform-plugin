package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.BackToBackTestReportsApi;
import org.openapitools.client.api.BackToBackTestsApi;
import org.openapitools.client.api.ExecutionConfigsApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.BackToBackTest;
import org.openapitools.client.model.BackToBackTest.VerdictStatusEnum;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Comparison;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcB2BStepExecution extends AbstractBtcStepExecution {

	private static final String REPORT_LINK_NAME_B2B = "Back-to-Back Test Report";
	private static final String REPORT_NAME_B2B = "BackToBackTestReport";
	private static final long serialVersionUID = 1L;
	private BtcB2BStep step;
	private BackToBackTestsApi b2bApi = new BackToBackTestsApi();
	private BackToBackTestReportsApi b2bReportingApi = new BackToBackTestReportsApi();
	private ScopesApi scopesApi = new ScopesApi();
	private ReportsApi reportingApi = new ReportsApi();
	private ExecutionConfigsApi execConfigApi = new ExecutionConfigsApi();
	
	private String suitename;

	public BtcB2BStepExecution(BtcB2BStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	@Override
	protected void performAction() throws Exception {
		// Check preconditions, retrieve scopes
		Scope toplevelScope = Util.getToplevelScope();

		// Prepare data for B2B test and execute B2B test
		BackToBackTestExecutionData data = prepareInfoObject();
		String b2bTestUid;
		try {
			log("Executing Back-to-Back Test %s vs. %s...", data.getRefMode(), data.getCompMode());
			suitename = "Back-to-Back-"+data.getRefMode()+"-vs-"+
					data.getCompMode()+"-"+Long.toString(System.currentTimeMillis());
			JUnitXMLHelper.addSuite(suitename);
			Job job = b2bApi.executeBackToBackTestOnScope(toplevelScope.getUid(), data);
			Map<?, ?> resultMap = (Map<?, ?>) HttpRequester.waitForCompletion(job.getJobID(), "result");
			b2bTestUid = (String) resultMap.get("uid");
		} catch (Exception e) {
			error("Failed to execute B2B test.", e);
			return;
		}

		// results and stuff
		parseResultsAndCreateReport(b2bTestUid);

	}

	/**
	 * Parses the results and creates the report.
	 */
	private void parseResultsAndCreateReport(String b2bTestUid) {
		// parse results
		try {
			BackToBackTest b2bTest = b2bApi.getTestByUID(b2bTestUid);
			parseResult(b2bTest);
		} catch (Exception e) {
			warning("Failed to parse the B2B test results.", e);
		}
		// create report
		try {
			generateAndExportReport(b2bTestUid);
		} catch (ApiException e) {
			warning("Failed to create the B2B test report.", e);
		}
		
	}

	/**
	 * Prepares the info object for rbt execution
	 * 
	 * @return
	 * @throws ApiException
	 */
	private BackToBackTestExecutionData prepareInfoObject() throws ApiException {
		BackToBackTestExecutionData data = new BackToBackTestExecutionData();
		List<String> executionConfigs = execConfigApi.getExecutionConfigs().getExecConfigNames();
		if (step.getReference() != null && step.getComparison() != null) {
			data.refMode(step.getReference()).compMode(step.getComparison());
		} else if (executionConfigs.size() >= 2) {
			// fallback: first config vs. second config
			data.refMode(executionConfigs.get(0)).compMode(executionConfigs.get(1));
		}
		return data;
	}

	private void parseResult(BackToBackTest b2bTest) {
		VerdictStatusEnum verdictStatus = b2bTest.getVerdictStatus();
		log("Back-to-Back Test finished with result: " + verdictStatus);
		// status, etc.
		String info = b2bTest.getComparisons().size() + " comparison(s), " + b2bTest.getPassed() + " passed, "
				+ b2bTest.getFailed() + " failed, " + b2bTest.getError() + " error(s)";
		info(info);
		
		for (Comparison comp : b2bTest.getComparisons()) {
			JUnitXMLHelper.Status testStatus = JUnitXMLHelper.Status.PASSED;
			switch(comp.getVerdictStatus()) {
				case ERROR:
					testStatus = JUnitXMLHelper.Status.ERROR;
					break;
				case FAILED:
					testStatus = JUnitXMLHelper.Status.FAILED;
					break;
				case FAILED_ACCEPTED:
					// not really sure what to do here? just treat it as failed i guess
					testStatus = JUnitXMLHelper.Status.FAILED;
					break;
			default:
				break;
			}
			JUnitXMLHelper.addTest(suitename, comp.getName(), testStatus, comp.getComment());
			
			
		}

		switch (verdictStatus) {
		case PASSED:
			status(Status.OK).passed().result("Passed");
			response = 200;
			break;
		case FAILED_ACCEPTED:
			status(Status.OK).passed().result("Failed accepted");
			response = 201;
			break;
		case FAILED:
			status(Status.OK).failed().result("Failed");
			response = 300;
			break;
		case ERROR:
			status(Status.ERROR).result("Error");
			response = 400;
			break;
		default:
			status(Status.ERROR).result("Unexpected Error");
			response = 500;
			break;
		}
	}

	/**
	 * @param b2bTestUid
	 * @throws ApiException
	 */
	private void generateAndExportReport(String b2bTestUid) throws ApiException {
		Report report = null;
		try {
			report = b2bReportingApi.createBackToBackReport(b2bTestUid);
		} catch (Exception e) {
			log("WARNING failed to create B2B report: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			warning();
		}
		ReportExportInfo reportExportInfo = new ReportExportInfo();
		reportExportInfo.exportPath(Store.exportPath).newName(REPORT_NAME_B2B);
		if (report != null) {
			try {
				reportingApi.exportReport(report.getUid(), reportExportInfo);
				detailWithLink(REPORT_LINK_NAME_B2B, REPORT_NAME_B2B + ".html");
			} catch (Exception e) {
				log("WARNING failed to export report: " + e.getMessage());
				try {
					log(((ApiException) e).getResponseBody());
				} catch (Exception idc) {
				}
				;
				warning();
			}
		}
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcB2BStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String reference;
	private String comparison;

	@DataBoundConstructor
	public BtcB2BStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcB2BStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

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
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getReference() {
		return reference;

	}

	@DataBoundSetter
	public void setReference(String reference) {
		this.reference = reference.toUpperCase();

	}

	public String getComparison() {
		return comparison;

	}

	@DataBoundSetter
	public void setComparison(String comparison) {
		this.comparison = comparison.toUpperCase();

	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
