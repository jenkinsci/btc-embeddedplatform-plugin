package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.api.CodeAnalysisReportsB2BApi;
import org.openapitools.client.api.CodeAnalysisReportsRbtApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcCodeAnalysisReportStepExecution extends StepExecution {

	private static final long serialVersionUID = 1L;
	private BtcCodeAnalysisReportStep step;
	private CodeAnalysisReportsRbtApi rbtReportApi = new CodeAnalysisReportsRbtApi();
	private CodeAnalysisReportsB2BApi b2bReportApi = new CodeAnalysisReportsB2BApi();
	private ReportsApi reportApi = new ReportsApi();
	
	public BtcCodeAnalysisReportStepExecution(BtcCodeAnalysisReportStep step, StepContext context) {
		super(context);
		this.step = step;
	}

//	@Override
//	protected void performAction() throws Exception {
//		// Check preconditions
//		String useCase = step.getUseCase();
//		checkArgument("B2B".equals(useCase) || "RBT".equals(useCase),
//				"ERROR: valid useCase for CodeAnalysisReport is RBT or B2B, not " + useCase);
//
//		Scope toplevel = Util.getToplevelScope();
//		Report report = null;
//		try {
//			log("Creating Code Analysis Report (%s)...", useCase);
//			if ("RBT".equals(useCase)) {
//				report = rbtReportApi.createCodeAnalysisReportOnScope1(toplevel.getUid());
//			} else { // B2B testing
//				report = b2bReportApi.createCodeAnalysisReportOnScope(toplevel.getUid());
//			}
//			ReportExportInfo info = new ReportExportInfo();
//			info.setNewName(step.getReportName());
//			info.setExportPath(Store.exportPath);
//			try {
//				reportApi.exportReport(report.getUid(), info);
//				String msg = "Exported the " + useCase + " coverage report.";
//				detailWithLink("Code Coverage Report", step.getReportName() + ".html");
//				info(msg);
//				log(msg);
//			} catch (Exception e) {
//				warning("Failed to export report.", e);
//			}
//		} catch (Exception e) {
//			warning("Report not generated.", e);
//		}
//	}

	@Override
	public boolean start() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcCodeAnalysisReportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private boolean includeSourceCode;
	private String reportName = "report";
	private String useCase = "B2B";

	@DataBoundConstructor
	public BtcCodeAnalysisReportStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcCodeAnalysisReportStepExecution(this, context);
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
			return "btcCodeAnalysisReport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Code Analysis Report Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */
	public boolean isIncludeSourceCode() {
		return includeSourceCode;
	}

	@DataBoundSetter
	public void setIncludeSourceCode(boolean includeSourceCode) {
		this.includeSourceCode = includeSourceCode;
	}

	public String getReportName() {
		return reportName;
	}

	@DataBoundSetter
	public void setReportName(String reportName) {
		this.reportName = reportName;
		if (this.reportName != null && this.reportName.endsWith(".html")) {
			this.reportName = this.reportName.substring(0, this.reportName.length() - 5);
		}
	}

	public String getUseCase() {
		return useCase;
	}

	@DataBoundSetter
	public void setUseCase(String useCase) {
		this.useCase = useCase;
	}
	/*
	 * End of getter/setter section
	 */

} // end of step class
