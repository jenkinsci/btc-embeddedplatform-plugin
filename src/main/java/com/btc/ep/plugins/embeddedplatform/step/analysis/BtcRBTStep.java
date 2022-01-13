package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.RequirementBasedTestExecutionApi;
import org.openapitools.client.api.RequirementBasedTestExecutionReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.RBTExecutionDataExtendedNoReport;
import org.openapitools.client.model.RBTExecutionDataNoReport;
import org.openapitools.client.model.RBTExecutionReportCreationInfo;
import org.openapitools.client.model.RBTExecutionReportCreationInfoData;
import org.openapitools.client.model.RBTestCaseExecutionResultSetData;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Requirement;
import org.openapitools.client.model.RequirementBasedTestCase;
import org.openapitools.client.model.RequirementSource;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.GenericResponse;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcRBTStepExecution extends AbstractBtcStepExecution {

    private static final String REPORT_LINK_NAME_RBT = "Test Execution Report";
    private static final String REPORT_NAME_RBT = "TestExecutionReport";
    private static final long serialVersionUID = 1L;
    private BtcRBTStep step;

    public BtcRBTStepExecution(BtcRBTStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    private RequirementBasedTestExecutionApi testExecutionApi = new RequirementBasedTestExecutionApi();
    private RequirementBasedTestExecutionReportsApi testExecutionReportApi =
        new RequirementBasedTestExecutionReportsApi();
    private ScopesApi scopesApi = new ScopesApi();
    private ProfilesApi profilesApi = new ProfilesApi();
    private ReportsApi reportingApi = new ReportsApi();

    @Override
    protected void performAction() throws Exception {
        // Check preconditions
        try {
            profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
            throw new IllegalStateException("You need an active profile to run tests");
        }

        // Prepare data
        List<String> tcUids = getRelevantTestCaseUIDs(); // <-- waiting for EP-2537
        RBTExecutionDataExtendedNoReport info = new RBTExecutionDataExtendedNoReport();
        RBTExecutionDataNoReport data = new RBTExecutionDataNoReport();
        List<String> executionConfigNames = Util.getValuesFromCsv(step.getExecutionConfigString());
        data.setForceExecute(false);
        data.setExecConfigNames(executionConfigNames);
        //TODO: the fallback should be: execution config list empty? -> execute on all configs (requires EP-2536)
        info.setUiDs(tcUids);
        info.setData(data);

        // Execute test and return result
        Job job = testExecutionApi.executeRBTOnRBTestCasesList(info);
        Object testResults = HttpRequester.waitForCompletion(job.getJobID(), "testResults");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, RBTestCaseExecutionResultSetData>>() {
        }.getType();
        JsonElement jsonElement = gson.toJsonTree(testResults);
        // map: result data by execution config
        Map<String, RBTestCaseExecutionResultSetData> testResultData = gson.fromJson(jsonElement, type);

        analyzeResults(testResultData, tcUids.isEmpty());

        generateAndExportReport(testResultData.keySet());

    }

    /**
     * Generates the reports (1 per execution config),
     * exports the html files and adds links to the overview report.
     * 
     * @param executionConfigs the execution configs
     * @throws ApiException
     */
    private void generateAndExportReport(Collection<String> executionConfigs) throws ApiException {
        if (step.isCreateReport()) {
            if (step.getReportSource().equalsIgnoreCase("REQUIREMENT")) {
                List<RequirementSource> requirementSources = Util.getRequirementSources();
                if (!requirementSources.isEmpty()) {
                    List<String> reqSourceUids =
                        requirementSources.stream().map(reqSource -> reqSource.getUid()).collect(Collectors.toList());
                    for (String executionConfig : executionConfigs) {
                        RBTExecutionReportCreationInfo data = new RBTExecutionReportCreationInfo();
                        data.setExecConfigName(executionConfig);
                        data.setUiDs(reqSourceUids);
                        Report report = testExecutionReportApi.createRBTExecutionReportOnRequirementsSourceList(data);
                        ReportExportInfo reportExportInfo = new ReportExportInfo();
                        reportExportInfo.exportPath(Store.exportPath)
                            .newName(REPORT_NAME_RBT + "-" + executionConfig.replace(" ", "_"));
                        reportingApi.exportReport(report.getUid(), reportExportInfo);
                        detailWithLink(REPORT_LINK_NAME_RBT + " (" + executionConfig + ")",
                            reportExportInfo.getNewName() + ".html");
                    }
                }
            } else {
                Scope toplevel = Util.getToplevelScope();
                for (String executionConfig : executionConfigs) {
                    RBTExecutionReportCreationInfoData data = new RBTExecutionReportCreationInfoData();
                    data.setExecConfigName(executionConfig);
                    Report report = testExecutionReportApi.createRBTExecutionReportOnScope(toplevel.getUid(), data);
                    ReportExportInfo reportExportInfo = new ReportExportInfo();
                    reportExportInfo.exportPath(Store.exportPath)
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
     * @param zeroTestCases a flag indicating whether any test cases matched the filter (false if no tests were run)
     */
    private void analyzeResults(Map<String, RBTestCaseExecutionResultSetData> testResultData, boolean zeroTestCases) {
        String overallResult = "PASSED";
        String infoText = "";
        for (String executionConfig : testResultData.keySet()) {
            RBTestCaseExecutionResultSetData resultData = testResultData.get(executionConfig);
            int noVerdict = Integer.parseInt(resultData.getNoVerdictTests());
            int errors = Integer.parseInt(resultData.getErrorneousTests());
            int total = Integer.parseInt(resultData.getTotalTests());
            if (errors > 0) {
                overallResult = "ERROR";
            } else if (Integer.parseInt(resultData.getFailedTests()) > 0) {
                overallResult = "FAILED";
            } else if (noVerdict == total) {
                overallResult = "NO_VERDICT"; // all test cases have "no verdict"
            }
            if (!infoText.isEmpty()) {
                infoText += "\n";
            }
            infoText += executionConfig + ": " + total + " tests, " + resultData.getPassedTests()
                + " passed, " + resultData.getFailedTests() + " failed";
            if (errors > 0) {
                infoText += ", " + errors + " error(s)";
            }
            if (noVerdict > 0) {
                infoText += ", " + noVerdict + " no verdict";
            }
        }
        info(infoText);
        jenkinsConsole.println("Requirements-based Test finished with result: " + overallResult);
        if ("PASSED".equalsIgnoreCase(overallResult)) {
            status(Status.OK).passed().result(overallResult);
            response = 200;
        } else if ("FAILED".equalsIgnoreCase(overallResult)) {
            status(Status.OK).failed().result(overallResult);
            response = 300;
        } else if (zeroTestCases) {
            status(Status.OK).skipped().result(overallResult);
            response = 300;
        } else if ("ERROR".equalsIgnoreCase(overallResult)) {
            status(Status.ERROR).result(overallResult);
            response = 400;
        } else {
            status(Status.ERROR).result(overallResult);
            response = 500;
        }
    }

    /**
     * Applies all available filters (requirements, scopes, tc names) and returns the matching test cases UIDs.
     *
     * @return a list with the matching test cases UIDs
     * @throws ApiException
     */
    private List<String> getRelevantTestCaseUIDs() throws ApiException {
        List<RequirementBasedTestCase> testCases = new ArrayList<>();
        List<Scope> scopes = scopesApi.getScopesByQuery1(null, false);

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
                filteredRequirements =
                    Util.filterRequirements(allRequirements, blacklistedRequirements,
                        whitelistedRequirements);
            }
        }

        checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");

        for (Scope scope : scopes) {
            if (Util.matchesScopeFilter(scope.getName(), blacklistedScopes, whitelistedScopes)) {
                /*
                 * Start of workaround for EP-2537
                 */
                //TODO: replace workaround for EP-2537
                GenericResponse genericResponse = HttpRequester.get("/ep/scopes/" + scope.getUid() + "/test-cases-rbt");
                if (genericResponse.getStatus().getStatusCode() == 200) {
                    List<Object> contentAsList = genericResponse.getContentAsList();
                    List<RequirementBasedTestCase> tcs = new ArrayList<>(contentAsList.size());
                    for (Object o : contentAsList) {
                        String tcInitStringWithUID = "{ \"uid\" : \"" + ((Map<?, ?>)o).get("uid").toString() + "\" }";
                        RequirementBasedTestCase tc =
                            new Gson().fromJson(tcInitStringWithUID, RequirementBasedTestCase.class);
                        tc.setName(((Map<?, ?>)o).get("name").toString());
                        tcs.add(tc);
                    }
                    List<RequirementBasedTestCase> filteredTestCases =
                        Util.filterTestCases(tcs, filteredRequirements, blacklistedTestCases, whitelistedTestCases);
                    testCases.addAll(filteredTestCases);
                }
                /*
                 * End of workaround for EP-2537
                 */
                //                List<RequirementBasedTestCase> filteredTestCases =
                //                    Util.filterTestCases(testCasesApi.getRBTestCasesByScope(scope.getUid()), filteredRequirements,
                //                        blacklistedTestCases, whitelistedTestCases);
            }
        }
        List<String> tcUids = testCases.stream().map(tc -> tc.getUid()).collect(Collectors.toList());
        return tcUids;
    }

    //    /**
    //     * @param b2bTestUid
    //     * @throws ApiException
    //     */
    //    private void generateAndExportReport(String b2bTestUid) throws ApiException {
    //        Report report = b2bReportingApi.createBackToBackReport(b2bTestUid);
    //        ReportExportInfo reportExportInfo = new ReportExportInfo();
    //        reportExportInfo.exportPath(Store.exportPath).newName(REPORT_NAME_B2B);
    //        reportingApi.exportReport(report.getUid(), reportExportInfo);
    //        detailWithLink(REPORT_LINK_NAME_B2B, REPORT_NAME_B2B + ".html");
    //    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcRBTStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String executionConfigString;
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
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
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

    /*
     * End of getter/setter section
     */

} // end of step class
