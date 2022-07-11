package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ExecutionConfigsApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.RequirementBasedTestExecutionApi;
import org.openapitools.client.api.RequirementBasedTestExecutionReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RBTExecutionDataExtendedNoReport;
import org.openapitools.client.model.RBTExecutionDataNoReport;
import org.openapitools.client.model.RBTExecutionReportCreationInfo;
import org.openapitools.client.model.RBTExecutionReportCreationInfoData;
import org.openapitools.client.model.RBTestCaseExecutionResultData;
import org.openapitools.client.model.RBTestCaseExecutionResultMapData;
import org.openapitools.client.model.RBTestCaseExecutionResultSetData;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Requirement;
import org.openapitools.client.model.RequirementBasedTestCase;
import org.openapitools.client.model.RequirementSource;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestCase;
import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestSuite;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.FilterHelper;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.Result;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcRBTStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcRBTStep step;
	
	public BtcRBTStepExecution(BtcRBTStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		RBTExecution exec = new RBTExecution(step, logger, getContext());
		
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

class RBTExecution extends BtcExecution {
	
	private static final long serialVersionUID = -6891468741557945109L;
	private static final String REPORT_LINK_NAME_RBT = "Test Execution Report";
	private static final String REPORT_NAME_RBT = "TestExecutionReport";
	
	private BtcRBTStep step;
	private RequirementBasedTestExecutionApi testExecutionApi = new RequirementBasedTestExecutionApi();
	private RequirementBasedTestExecutionReportsApi testExecutionReportApi = new RequirementBasedTestExecutionReportsApi();
	private ScopesApi scopesApi = new ScopesApi();
	private ReportsApi reportingApi = new ReportsApi();
	private RequirementBasedTestCasesApi testCasesApi = new RequirementBasedTestCasesApi();
	private ExecutionConfigsApi ecApi = new ExecutionConfigsApi();

	public RBTExecution(BtcRBTStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	@Override
	protected Object performAction() throws Exception {
		// Prepare data
		Set<String> tcUids = getRelevantTestCaseUIDs();

		// skip rbt execution if there are no test cases
		if (tcUids.isEmpty()) {
			log("Skipping Requirements-based Test execution. No test cases are matching the filters");
			info("No test cases are matching the filters");
			skipped();
			return response(300);
		}

		// RBT Execution
		RBTExecutionDataExtendedNoReport info = prepareInfoObject(tcUids);
		Job job = testExecutionApi.executeRBTOnRBTestCasesList(info);
		RBTestCaseExecutionResultMapData testResults = HttpRequester.waitForCompletion(job.getJobID(), "result",
				RBTestCaseExecutionResultMapData.class);

		// results + repo
		dataTransferObject.testSuite = new JUnitXmlTestSuite("RBT");
		parseResultsAndCreateReport(tcUids, testResults);
		return response(200);
	}

	/**
	 * Parses the results and creates the report.
	 * 
	 * @param tcUids
	 * @param testResults
	 */
	private void parseResultsAndCreateReport(Set<String> tcUids, RBTestCaseExecutionResultMapData testResults) {
		if (testResults != null) {
			try {
				analyzeResults(testResults, tcUids.isEmpty());
			} catch (Exception e) {
				warning("Failed to parse Test Results.", e);
			}
			try {
				generateAndExportReport(testResults);
			} catch (Exception e) {
				warning("Failed to create the report.", e);
			}
			Map<String, RBTestCaseExecutionResultSetData> tr = testResults.getTestResults();
			
			for (String key: tr.keySet()) {
				for(RBTestCaseExecutionResultData result : tr.get(key).getTestResults()) {
					JUnitXMLHelper.Status testStatus = JUnitXMLHelper.Status.PASSED;
					switch(result.getVerdictStatus()) {
						case "Failed":
							testStatus = JUnitXMLHelper.Status.FAILED;
							break;
						default:
							break;
					}
					String testname = null;
					try {
						testname = testCasesApi.getRBTestCase(result.getRbTestCaseUID()).getName();
					} catch (ApiException e) {
						warning("Couldn't get test-case name for UID " + result.getRbTestCaseUID());
					}
					dataTransferObject.testSuite.testCases.add(new JUnitXmlTestCase(key+"-"+testname, testStatus, result.getComment()));
				}
			}
		}
	}

	/**
	 * Prepares the info object for rbt execution
	 */
	private RBTExecutionDataExtendedNoReport prepareInfoObject(Set<String> tcUids) throws ApiException {
		RBTExecutionDataExtendedNoReport info = new RBTExecutionDataExtendedNoReport();
		RBTExecutionDataNoReport data = new RBTExecutionDataNoReport();
		List<String> executionConfigNames = Util.getValuesFromCsv(step.getExecutionConfigString());
		if (executionConfigNames.isEmpty()) {
			executionConfigNames = ecApi.getExecutionConfigs().getExecConfigNames();
		}
		log("Executing Requirements-based Tests on %s...", executionConfigNames);
		data.setForceExecute(false);
		data.setExecConfigNames(executionConfigNames);
		info.setUiDs(new ArrayList<>(tcUids));
		info.setData(data);
		return info;
	}

	/**
	 * Generates the reports (1 per execution config), exports the html files and
	 * adds links to the overview report.
	 * 
	 * @param executionConfigs the execution configs
	 * @throws ApiException
	 */
	private void generateAndExportReport(RBTestCaseExecutionResultMapData result) throws ApiException {
		if (step.isCreateReport()) {
			if (step.getReportSource().equalsIgnoreCase("REQUIREMENT")) {
				List<RequirementSource> requirementSources = Util.getRequirementSources();
				if (!requirementSources.isEmpty()) {
					List<String> reqSourceUids = requirementSources.stream().map(reqSource -> reqSource.getUid())
							.collect(Collectors.toList());
					for (String executionConfig : result.getTestResults().keySet()) {
						RBTExecutionReportCreationInfo data = new RBTExecutionReportCreationInfo();
						data.setExecConfigName(executionConfig);
						data.setUiDs(reqSourceUids);
						Report report = testExecutionReportApi.createRBTExecutionReportOnRequirementsSourceList(data);
						ReportExportInfo reportExportInfo = new ReportExportInfo();
						reportExportInfo.exportPath(dataTransferObject.exportPath)
								.newName(REPORT_NAME_RBT + "-" + executionConfig.replace(" ", "_"));
						reportingApi.exportReport(report.getUid(), reportExportInfo);
						detailWithLink(REPORT_LINK_NAME_RBT + " (" + executionConfig + ")",
								reportExportInfo.getNewName() + ".html");
					}
				}
			} else {
				Scope toplevel = Util.getToplevelScope();
				for (String executionConfig : result.getTestResults().keySet()) {
					RBTExecutionReportCreationInfoData data = new RBTExecutionReportCreationInfoData();
					data.setExecConfigName(executionConfig);
					Report report = testExecutionReportApi.createRBTExecutionReportOnScope(toplevel.getUid(), data);
					ReportExportInfo reportExportInfo = new ReportExportInfo();
					reportExportInfo.exportPath(dataTransferObject.exportPath)
							.newName(REPORT_NAME_RBT + "-" + executionConfig.replace(" ", "_"));
					reportingApi.exportReport(report.getUid(), reportExportInfo);
					detailWithLink(REPORT_LINK_NAME_RBT + " (" + executionConfig + ")",
							reportExportInfo.getNewName() + ".html");
				}

			}

		}
	}

	/**
	 * Analyzes the result data, sets the info text & result for the report.
	 *
	 * @param testResultData the test result data
	 * @param zeroTestCases  a flag indicating whether any test cases matched the
	 *                       filter (false if no tests were run)
	 */
	private void analyzeResults(RBTestCaseExecutionResultMapData testResultData, boolean zeroTestCases) {
		Result overallResult = Result.PASSED;
		String infoText = "";
		for (Entry<String, RBTestCaseExecutionResultSetData> entry : testResultData.getTestResults().entrySet()) {
			String executionConfig = entry.getKey();
			RBTestCaseExecutionResultSetData resultData = entry.getValue();
			int noVerdict = Integer.parseInt(resultData.getNoVerdictTests());
			int errors = Integer.parseInt(resultData.getErrorneousTests());
			int total = Integer.parseInt(resultData.getTotalTests());
			if (errors > 0) {
				overallResult = Result.ERROR;
			} else if (Integer.parseInt(resultData.getFailedTests()) > 0) {
				overallResult = Result.FAILED;
			} else if (noVerdict == total) {
				overallResult = Result.NO_VERDICT; // all test cases have "no verdict"
			}
			if (!infoText.isEmpty()) {
				infoText += "\n";
			}
			infoText += executionConfig + ": " + total + " tests, " + resultData.getPassedTests() + " passed, "
					+ resultData.getFailedTests() + " failed";
			if (errors > 0) {
				infoText += ", " + errors + " error(s)";
			}
			if (noVerdict > 0) {
				infoText += ", " + noVerdict + " no verdict";
			}
		}
		info(infoText);
		log("Requirements-based Test finished with result: " + overallResult);
		if (Result.PASSED.equals(overallResult)) {
			status(Status.OK).passed().result("Passed");
			response(200);
		} else if (Result.FAILED.equals(overallResult)) {
			status(Status.OK).failed().result("Failed");
			response(300);
		} else if (zeroTestCases) {
			status(Status.OK).skipped().result("Skipped");
			response(300);
		} else if (Result.ERROR.equals(overallResult)) {
			status(Status.ERROR).result("Error");
			response(400);
		} else {
			status(Status.ERROR).result("Error");
			response(500);
		}
	}

	/**
	 * Applies all available filters (requirements, scopes, tc names) and returns
	 * the matching test cases UIDs.
	 *
	 * @return a list with the matching test cases UIDs
	 * @throws ApiException
	 */
	private Set<String> getRelevantTestCaseUIDs() throws ApiException {
		List<Scope> scopes = scopesApi.getScopesByQuery1(null, FALSE);

		List<String> blacklistedRequirements = Util.getValuesFromCsv(step.getRequirementsBlacklist());
		List<String> whitelistedRequirements = Util.getValuesFromCsv(step.getRequirementsWhitelist());
		List<String> blacklistedTestCases = Util.getValuesFromCsv(step.getTestCasesBlacklist());
		List<String> whitelistedTestCases = Util.getValuesFromCsv(step.getTestCasesWhitelist());
		List<String> blacklistedScopes = Util.getValuesFromCsv(step.getScopesBlacklist());
		List<String> whitelistedScopes = Util.getValuesFromCsv(step.getScopesWhitelist());

		List<Requirement> filteredRequirements = null;
		if (!blacklistedRequirements.isEmpty() || !whitelistedRequirements.isEmpty()) {
			List<Requirement> allRequirements = Util.getAllRequirements();
			if (!allRequirements.isEmpty()) {
				filteredRequirements = FilterHelper.filterRequirements(allRequirements, blacklistedRequirements,
						whitelistedRequirements);
			}
		}

		checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");

		List<RequirementBasedTestCase> filteredTestCases = new ArrayList<>();
		for (Scope scope : scopes) {
			if (FilterHelper.matchesScopeFilter(scope.getName(), blacklistedScopes, whitelistedScopes)) {
				try {
					List<RequirementBasedTestCase> testCasesByScope = testCasesApi
							.getRBTestCasesByScope(scope.getUid());
					filteredTestCases.addAll(FilterHelper.filterTestCases(testCasesByScope, filteredRequirements,
							blacklistedTestCases, whitelistedTestCases));
				} catch (ApiException e) {
					// TODO: can be removed once the GET request return an empty list instead of an
					// error
					if (e.getMessage().contains("Not Found")) {
						continue; // no tests for this scope, so we just pass over it.
					}
					// else:
					throw e;
				}
			}
		}
		Set<String> tcUids = filteredTestCases.stream().map(tc -> tc.getUid().toString()).collect(Collectors.toSet());
		return tcUids;
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcRBTStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String executionConfigString;
	private List<String> executionConfigs;
	private String reportSource = "SCOPE";
	private boolean createReport = false;
	private String scopesWhitelist;
	private String scopesBlacklist;
	private String requirementsWhitelist;
	private String requirementsBlacklist;
	private String foldersWhitelist;
	private String foldersBlacklist;
	private String testCasesWhitelist;
	private String testCasesBlacklist;

	@DataBoundConstructor
	public BtcRBTStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcRBTStepExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcRbtExecution";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "Requirements Test with BTC EmbeddedPlatform";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getExecutionConfigString() {
		return executionConfigString;
	}

	public String getReportSource() {
		return reportSource;
	}

	public boolean isCreateReport() {
		return createReport;
	}

	public String getScopesWhitelist() {
		return scopesWhitelist;
	}

	public String getScopesBlacklist() {
		return scopesBlacklist;
	}

	public String getRequirementsWhitelist() {
		return requirementsWhitelist;
	}

	public String getRequirementsBlacklist() {
		return requirementsBlacklist;
	}

	public String getFoldersWhitelist() {
		return foldersWhitelist;
	}

	public String getFoldersBlacklist() {
		return foldersBlacklist;
	}

	public String getTestCasesWhitelist() {
		return testCasesWhitelist;
	}

	public String getTestCasesBlacklist() {
		return testCasesBlacklist;
	}

	public List<String> getExecutionConfigs() {
		return executionConfigs;
	}

	@DataBoundSetter
	public void setExecutionConfigString(String executionConfigString) {
		this.executionConfigString = executionConfigString;
	}

	@DataBoundSetter
	public void setReportSource(String reportSource) {
		this.reportSource = reportSource;
	}

	@DataBoundSetter
	public void setCreateReport(boolean createReport) {
		this.createReport = createReport;
	}

	@DataBoundSetter
	public void setScopesWhitelist(String scopesWhitelist) {
		this.scopesWhitelist = scopesWhitelist;
	}

	@DataBoundSetter
	public void setScopesBlacklist(String scopesBlacklist) {
		this.scopesBlacklist = scopesBlacklist;
	}

	@DataBoundSetter
	public void setRequirementsWhitelist(String requirementsWhitelist) {
		this.requirementsWhitelist = requirementsWhitelist;
	}

	@DataBoundSetter
	public void setRequirementsBlacklist(String requirementsBlacklist) {
		this.requirementsBlacklist = requirementsBlacklist;
	}

	@DataBoundSetter
	public void setFoldersWhitelist(String foldersWhitelist) {
		this.foldersWhitelist = foldersWhitelist;
	}

	@DataBoundSetter
	public void setFoldersBlacklist(String foldersBlacklist) {
		this.foldersBlacklist = foldersBlacklist;
	}

	@DataBoundSetter
	public void setTestCasesWhitelist(String testCasesWhitelist) {
		this.testCasesWhitelist = testCasesWhitelist;
	}

	@DataBoundSetter
	public void setTestCasesBlacklist(String testCasesBlacklist) {
		this.testCasesBlacklist = testCasesBlacklist;
	}

	@DataBoundSetter
	public void setExecutionConfigs(List<String> executionConfigs) {
		this.executionConfigs = executionConfigs;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
