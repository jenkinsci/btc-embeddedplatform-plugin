package com.btc.ep.plugins.embeddedplatform.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.RequirementBasedTestExecutionApi;
import org.openapitools.client.api.RequirementBasedTestExecutionReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Profile;
import org.openapitools.client.model.ProfilePath;
import org.openapitools.client.model.RBTExecutionDataExtendedNoReport;
import org.openapitools.client.model.RBTExecutionDataNoReport;
import org.openapitools.client.model.RBTExecutionReportCreationInfo;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.ApiClientThatDoesntSuck;
import com.btc.ep.plugins.embeddedplatform.http.GenericResponse;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;

public class TestStuff {

    private static ApiClient apiClient = new ApiClientThatDoesntSuck().setBasePath("http://localhost:29267");

    public static void main(String[] args) throws IOException, InterruptedException {
        try {

            Configuration.setDefaultApiClient(apiClient);

            ProfilesApi profilesApi = new ProfilesApi();
            try {
                profilesApi.saveProfile(new ProfilePath().path("E:/deleteme"));
                Files.delete(Paths.get("E:/deleteme"));
            } catch (Exception e) {
                //ignored
            }
            Profile profile =
                profilesApi.openProfile("E:/profile_29.epp");
            System.out.println(profile);
            //FIXME: workaround for http://jira.osc.local:8080/browse/EP-2355 (#1)
            Job updateJob = new ArchitecturesApi().architectureUpdate();
            HttpRequester.waitForCompletion(updateJob.getJobID());

            //FIXME: workaround for http://jira.osc.local:8080/browse/EP-2355 (#2)
            //            List<RequirementBasedTestCase> testCases =
            //                new RequirementBasedTestCasesApi().getAllRBTestCases();
            //List<String> tcUids = testCases.stream().map(tc -> tc.getUid()).collect(Collectors.toList());
            GenericResponse r = HttpRequester.get("/ep/test-cases-rbt");
            List<String> tcUids =
                r.getContentAsList().stream().map(m -> (String)((Map<?, ?>)m).get("uid")).collect(Collectors.toList());
            System.out.println(tcUids);
            RBTExecutionDataExtendedNoReport rbtExecution = new RBTExecutionDataExtendedNoReport();
            RBTExecutionDataNoReport data = new RBTExecutionDataNoReport();
            data.setExecConfigNames(Arrays.asList("SIL"));
            rbtExecution.setUiDs(tcUids);
            rbtExecution.setData(data);
            Job executionJob =
                new RequirementBasedTestExecutionApi().executeRBTOnRBTestCasesList(rbtExecution);
            HttpRequester.waitForCompletion(executionJob.getJobID());
            RequirementBasedTestExecutionReportsApi reportsApi =
                new RequirementBasedTestExecutionReportsApi();
            List<Scope> scopes = new ScopesApi().getScopesByQuery1(null, false);
            RBTExecutionReportCreationInfo info = new RBTExecutionReportCreationInfo();
            info.setExecConfigName("SIL");
            info.setUiDs(scopes.stream().map(sc -> sc.getUid()).collect(Collectors.toList()));
            Report report = reportsApi.createRBTExecutionReportOnScopeList(info);
            ReportExportInfo reportExportInfo = new ReportExportInfo();
            reportExportInfo.setExportPath("E:/reports");
            new ReportsApi().exportReport(report.getUid(), reportExportInfo);
        } catch (ApiException e) {
            System.err.println(e.getResponseBody());
            e.printStackTrace();

        }
    }

}
