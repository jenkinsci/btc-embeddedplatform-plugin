package com.btc.ep.plugins.embeddedplatform.step.analysis;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.CodeAnalysisReportsB2BApi;
import org.openapitools.client.api.CodeAnalysisReportsRbtApi;
import org.openapitools.client.api.CodeCoverageRobustnessCheckB2BApi;
import org.openapitools.client.api.CodeCoverageRobustnessCheckRbtApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.model.CodeCoverageResult;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcCodeAnalysisReportStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcCodeAnalysisReportStep step;
	
	public BtcCodeAnalysisReportStepExecution(BtcCodeAnalysisReportStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		CodeAnalysisReportExecution exec = new CodeAnalysisReportExecution(step, logger, getContext());
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
	
}

class CodeAnalysisReportExecution extends BtcExecution {
	
	public CodeAnalysisReportExecution(BtcCodeAnalysisReportStep step, PrintStream logger, StepContext context) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}

	private static final long serialVersionUID = 5466888085263278414L;
	private BtcCodeAnalysisReportStep step;
	
	private transient CodeAnalysisReportsRbtApi rbtReportApi;
	private transient CodeAnalysisReportsB2BApi b2bReportApi;
	private transient CodeCoverageRobustnessCheckRbtApi rbtCovApi;
	private transient CodeCoverageRobustnessCheckB2BApi b2bCovApi;
	private transient ReportsApi reportApi;

	@Override
	protected Object performAction() throws Exception {
		rbtReportApi = new CodeAnalysisReportsRbtApi();
		b2bReportApi = new CodeAnalysisReportsB2BApi();
		rbtCovApi = new CodeCoverageRobustnessCheckRbtApi();
		b2bCovApi = new CodeCoverageRobustnessCheckB2BApi();
		reportApi = new ReportsApi();
		
		// Check preconditions
		String useCase = step.getUseCase();
		checkArgument("B2B".equals(useCase) || "RBT".equals(useCase),
				"ERROR: valid useCase for CodeAnalysisReport is RBT or B2B, not " + useCase);

		Scope toplevel = Util.getToplevelScope();
		Report report = null;
		log("Creating Code Analysis Report (%s)...", useCase);
		if ("RBT".equals(useCase)) {
			report = rbtReportApi.createCodeAnalysisReportOnScope1(toplevel.getUid());
		} else { // B2B testing
			report = b2bReportApi.createCodeAnalysisReportOnScope(toplevel.getUid());
		}
		ReportExportInfo info = new ReportExportInfo();
		info.setNewName(step.getReportName());
		info.setExportPath(dataTransferObject.exportPath);
		reportApi.exportReport(report.getUid(), info);
		detailWithLink("Code Coverage Report", step.getReportName() + ".html");
		String msg = getMessage(useCase);
		info(msg);
		log(msg);
		return getResponse();
	}

	private String getMessage(String useCase) throws ApiException {
		List<String> goalTypesList = Arrays.asList("STM", "MCDC", "CA", "DZ");
		CodeCoverageResult codeCoverageResult;
		if ("RBT".equals(useCase)) {
			codeCoverageResult = rbtCovApi.getCodeCoverageResultByScope1(Util.getToplevelScope().getUid(), goalTypesList);
		} else {
			codeCoverageResult = b2bCovApi.getCodeCoverageResultByScope(Util.getToplevelScope().getUid(), goalTypesList);
		}
		BigDecimal stmBD = codeCoverageResult.getStatementCoverage().getHandledCompletelyPercentage();
		BigDecimal mcdcBD = codeCoverageResult.getMcDCCoverage().getHandledCompletelyPercentage();
		
		long covSumDivByZero = codeCoverageResult.getDivisionByZeroPropertyCoverage().getCoveredCount();
		long covSumDowncast = codeCoverageResult.getDownCastPropertyCoverage().getCoveredCount();
		
		Double stmD = stmBD == null ? null : Math.round((stmBD.doubleValue() * 100)) / 100d;
        Double mcdcD = mcdcBD == null ? null : Math.round((mcdcBD.doubleValue() * 100)) / 100d;
//        exportCoverageOverviewCsv(stmD, dD, mcdcD, reportDir, useCase);
        String info = stmD + "% Statement, " + mcdcD + "% MC/DC";
        if ((covSumDivByZero + covSumDowncast) > 0) {
            if (covSumDivByZero > 0) {
                info += ", " + covSumDivByZero + " divisions by zero";
            }
            if (covSumDowncast > 0) {
                info += ", " + covSumDowncast + " downcasts";
            }
        }
        return info;
		
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
