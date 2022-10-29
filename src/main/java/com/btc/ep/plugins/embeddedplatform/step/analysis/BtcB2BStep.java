package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.BackToBackTestReportsApi;
import org.openapitools.client.api.BackToBackTestsApi;
import org.openapitools.client.api.ExecutionConfigsApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.RestBackToBackTest;
import org.openapitools.client.model.RestBackToBackTest.VerdictStatusEnum;
import org.openapitools.client.model.RestComparison;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestCase;
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestSuite;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcB2BStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcB2BStep step;

	public BtcB2BStepExecution(BtcB2BStep step, StepContext context) {
		super(context);
		this.step = step;
	}
	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		B2BExecution exec = new B2BExecution(step, logger, getContext());
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// do JUnit stuff on jenkins controller
		JUnitXMLHelper.addSuite(stepResult.testSuite.suiteName);
		for (JUnitXmlTestCase tc : stepResult.testSuite.testCases) {
			JUnitXMLHelper.addTest(stepResult.testSuite.suiteName, tc.name, tc.status, tc.message);
		}
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class B2BExecution extends BtcExecution {
	
	private static final long serialVersionUID = -8348842225440464449L;
	private static final String REPORT_LINK_NAME_B2B = "Back-to-Back Test Report";
	private static final String REPORT_NAME_B2B = "RestBackToBackTestReport";
	private String suitename;

	private BtcB2BStep step;
	
	// transient fields for required services
	private transient BackToBackTestsApi b2bApi = new BackToBackTestsApi();
	private transient BackToBackTestReportsApi b2bReportingApi = new BackToBackTestReportsApi();
	private transient ReportsApi reportingApi = new ReportsApi();
	private transient ExecutionConfigsApi execConfigApi = new ExecutionConfigsApi();
	
	public B2BExecution(BtcB2BStep step, PrintStream logger, StepContext context) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}
	
	@Override
	protected Object performAction() throws Exception {
		b2bApi = new BackToBackTestsApi();
		b2bReportingApi = new BackToBackTestReportsApi();
		reportingApi = new ReportsApi();
		execConfigApi = new ExecutionConfigsApi();
		// Check preconditions, retrieve scopes
		Scope toplevelScope = Util.getToplevelScope();

		// Prepare data for B2B test and execute B2B test
		BackToBackTestExecutionData data = prepareInfoObject();
		String b2bTestUid;
		try {
			log("Executing Back-to-Back Test %s vs. %s...", data.getRefMode(), data.getCompMode());
			suitename = "Back-to-Back-"+data.getRefMode()+"-vs-"+
					data.getCompMode()+"-"+Long.toString(System.currentTimeMillis());
			dataTransferObject.testSuite = new JUnitXmlTestSuite(suitename);
			Job job = b2bApi.executeBackToBackTestOnScope(toplevelScope.getUid(), data);
			Map<?, ?> resultMap = (Map<?, ?>) HttpRequester.waitForCompletion(job.getJobID(), "result");
			b2bTestUid = (String) resultMap.get("uid");
		} catch (Exception e) {
			error("Failed to execute B2B test.", e);
			return response(500);
		}

		// results and stuff
		parseResultsAndCreateReport(b2bTestUid);
		return getResponse();
	}

	/**
	 * Parses the results and creates the report.
	 */
	private void parseResultsAndCreateReport(String b2bTestUid) {
		// parse results
		try {
			RestBackToBackTest b2bTest;
			if (b2bTestUid == null) {
				List<RestBackToBackTest> allB2bTests = b2bApi.getAllTests();
				b2bTest = allB2bTests.get(allB2bTests.size() - 1);
			} else {
				b2bTest = b2bApi.getTestByUID(b2bTestUid);
			}
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

	private void parseResult(RestBackToBackTest b2bTest) {
		VerdictStatusEnum verdictStatus = b2bTest.getVerdictStatus();
		log("Back-to-Back Test finished with result: " + verdictStatus);
		// status, etc.
		String info = b2bTest.getComparisons().size() + " comparison(s), " + b2bTest.getPassed() + " passed, "
				+ b2bTest.getFailed() + " failed, " + b2bTest.getError() + " error(s)";
		info(info);
		
		for (RestComparison comp : b2bTest.getComparisons()) {
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
			dataTransferObject.testSuite.testCases.add(new JUnitXmlTestCase(comp.getName(), testStatus, comp.getComment()));
			
			
		}
		// overall result
		switch (verdictStatus) {
		case PASSED:
			status(Status.OK).passed().result("Passed");
			response(200);
			break;
		case FAILED_ACCEPTED:
			status(Status.OK).passed().result("Failed accepted");
			response(201);
			break;
		case FAILED:
			status(Status.OK).failed().result("Failed");
			response(300);
			break;
		case ERROR:
			status(Status.ERROR).result("Error");
			response(400);
			break;
		default:
			status(Status.ERROR).result("Unexpected Error");
			response(500);
			break;
		}
	}

	/**
	 * @param b2bTestUid
	 * @throws ApiException
	 */
	private void generateAndExportReport(String b2bTestUid) throws ApiException {
		Report report = b2bReportingApi.createBackToBackReport(b2bTestUid);
		ReportExportInfo reportExportInfo = new ReportExportInfo();
		reportExportInfo.exportPath(dataTransferObject.exportPath).newName(REPORT_NAME_B2B);
		if (report != null) {
			reportingApi.exportReport(report.getUid(), reportExportInfo);
			detailWithLink(REPORT_LINK_NAME_B2B, REPORT_NAME_B2B + ".html");
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
