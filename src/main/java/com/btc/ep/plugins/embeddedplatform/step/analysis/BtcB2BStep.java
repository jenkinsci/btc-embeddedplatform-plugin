package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

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
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.BackToBackTest;
import org.openapitools.client.model.BackToBackTest.VerdictStatusEnum;
import org.openapitools.client.model.BackToBackTestExecutionData;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;

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

	public BtcB2BStepExecution(BtcB2BStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	private BackToBackTestsApi b2bApi = new BackToBackTestsApi();
	private BackToBackTestReportsApi b2bReportingApi = new BackToBackTestReportsApi();
	private ScopesApi scopesApi = new ScopesApi();
	private ProfilesApi profilesApi = new ProfilesApi();
	private ReportsApi reportingApi = new ReportsApi();
	private ExecutionConfigsApi execConfigApi = new ExecutionConfigsApi();

	@Override
	protected void performAction() throws Exception {
		// Check preconditions
		try {
			profilesApi.getCurrentProfile(); // throws Exception if no profile is active
		} catch (Exception e) {
			throw new IllegalStateException("You need an active profile to perform a Back-to-Back Test");
		}
		List<Scope> scopes = null;
		try {
			scopes = scopesApi.getScopesByQuery1(null, true);
		} catch (Exception e) {
			log("ERROR getting scopes: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
		}
		checkArgument(!scopes.isEmpty(), "The profile contains no scopes.");
		Scope toplevelScope = scopes.get(0);

		// Prepare data for B2B test
		BackToBackTestExecutionData data = new BackToBackTestExecutionData();
		List<String> executionConfigs = execConfigApi.getExecutionRecords().getExecConfigNames();
		if (step.getReference() != null && step.getComparison() != null) {
			data.refMode(step.getReference()).compMode(step.getComparison());
		} else if (executionConfigs.size() >= 2) {
			// fallback: first config vs. second config
			data.refMode(executionConfigs.get(0)).compMode(executionConfigs.get(1));
		}

		// Execute B2B test and return result
		Job job = null;
		try {
			log("Executing Back-to-Back Test %s vs. %s...", data.getRefMode(), data.getCompMode());
			job = b2bApi.executeBackToBackTestOnScope(toplevelScope.getUid(), data);
		} catch (Exception e) {
			log("Error: failed to execute B2B test: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
		}
		Map<?, ?> resultMap = (Map<?, ?>) HttpRequester.waitForCompletion(job.getJobID(), "result");

		String b2bTestUid = (String) resultMap.get("uid");
		try {
			BackToBackTest b2bTest = b2bApi.getTestByUID(b2bTestUid);
			parseResult(b2bTest);
			// detail with link happens internally in the report func
			generateAndExportReport(b2bTestUid);
		} catch (Exception e) {
			log("ERROR executing B2B tests: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
			error();
		}

	}

	private void parseResult(BackToBackTest b2bTest) {
		String verdictStatus = b2bTest.getVerdictStatus().toString();
		log("Back-to-Back Test finished with result: " + verdictStatus);
		// status, etc.
		String info = b2bTest.getComparisons().size() + " comparison(s), " + b2bTest.getPassed() + " passed, "
				+ b2bTest.getFailed() + " failed, " + b2bTest.getError() + " error(s)";
		info(info);

		if (VerdictStatusEnum.PASSED.name().equalsIgnoreCase(verdictStatus)) {
			status(Status.OK).passed().result(verdictStatus);
			response = 200;
		} else if (VerdictStatusEnum.FAILED_ACCEPTED.name().equalsIgnoreCase(verdictStatus)) {
			status(Status.OK).passed().result(verdictStatus);
			response = 201;
		} else if (VerdictStatusEnum.FAILED.name().equalsIgnoreCase(verdictStatus)) {
			status(Status.OK).failed().result(verdictStatus);
			response = 300;
		} else if (VerdictStatusEnum.ERROR.name().equalsIgnoreCase(verdictStatus)) {
			status(Status.ERROR).result(verdictStatus);
			response = 400;
		} else {
			status(Status.ERROR).result(verdictStatus);
			response = 500;
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

		/*
		 * This specifies the step name that the the user can use in his Jenkins
		 * Pipeline - for example: btcStartup installPath: 'C:/Program
		 * Files/BTC/ep2.9p0', port: 29267
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
