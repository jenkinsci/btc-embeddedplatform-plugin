package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.Serializable;
import java.util.Arrays;
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
import org.openapitools.client.api.CodeAnalysisReportsB2BApi;
import org.openapitools.client.api.CodeCoverageRobustnessCheckB2BApi;
import org.openapitools.client.api.CoverageGenerationApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.CodeCoverageResult;
import org.openapitools.client.model.Config;
import org.openapitools.client.model.Config.CheckUnreachablePropertiesEnum;
import org.openapitools.client.model.Config.IsSubscopesGoalsConsideredEnum;
import org.openapitools.client.model.CoreEngine;
import org.openapitools.client.model.CoreEngine.NameEnum;
import org.openapitools.client.model.CoreEngine.UseEnum;
import org.openapitools.client.model.EngineAtg;
import org.openapitools.client.model.EngineAtg.ExecutionModeEnum;
import org.openapitools.client.model.EngineCv;
import org.openapitools.client.model.EngineCv.ParallelExecutionModeEnum;
import org.openapitools.client.model.EngineCv.SearchFocusEnum;
import org.openapitools.client.model.EngineSettings;
import org.openapitools.client.model.EngineSettings.AllowDenormalizedFloatsEnum;
import org.openapitools.client.model.EngineSettings.AnalyseSubScopesHierarchicallyEnum;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;
import org.openapitools.client.model.TargetDefinition.EnabledEnum;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorGenerationExecution extends AbstractBtcStepExecution {

	private static final long serialVersionUID = 1L;
	private BtcVectorGenerationStep step;
	private CoverageGenerationApi vectorGenerationApi = new CoverageGenerationApi();
	private ScopesApi scopeApi = new ScopesApi();
	private ReportsApi reportApi = new ReportsApi();
	private CodeAnalysisReportsB2BApi b2bCodeAnalysisReportApi = new CodeAnalysisReportsB2BApi();
	private CodeCoverageRobustnessCheckB2BApi coverageApi = new CodeCoverageRobustnessCheckB2BApi();
	
	public BtcVectorGenerationExecution(BtcVectorGenerationStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	@Override
	protected void performAction() throws Exception {
		// Preparation
		Scope toplevelScope = Util.getToplevelScope();
		
		// Vector Generation
		try {
			log("Generating Vectors...");
			prepareAndExecuteVectorGeneration();
		} catch (Exception e) {
			warning("Failed to execute vector generation.", e);
			return;
		}

		// Reporting
		reporting(toplevelScope);
	}

	private void reporting(Scope toplevel) throws ApiException {
		String msg = "Successfully executed vectorGeneration";
		CodeCoverageResult codeCoverageResult = coverageApi.getCodeCoverageResultByScope(toplevel.getUid(), Arrays.asList("MCDC","STM"));
		double stmD = codeCoverageResult.getStatementCoverage().getCoveredCompletelyPercentage().doubleValue();
		double mcdcD = codeCoverageResult.getMcDCCoverage().getCoveredCompletelyPercentage().doubleValue();
		String info = stmD + "% Statement, " + mcdcD + "% MC/DC";
		if (step.isCreateReport()) {
			try {
				Report report = b2bCodeAnalysisReportApi.createCodeAnalysisReportOnScope(toplevel.getUid());
				ReportExportInfo reportInfo = new ReportExportInfo();
				String reportName = "CodeCoverageReport";
				reportInfo.setNewName(reportName);
				reportInfo.setExportPath(Store.exportPath);
				reportApi.exportReport(report.getUid(), reportInfo);
				msg += " and exported the coverage report";
				detailWithLink("Code Coverage Report", reportName + ".html");
			} catch (Exception e) {
				warning("Failed to create report.", e);
			}
			info(info);
		}
		log(msg + ": " + info);
	}

	private boolean isDummyRoot(ScopesApi scopeApi) throws ApiException {
		try {
			scopeApi.getScopesByQuery1(null, TRUE);
		} catch (ApiException e) {
			if (404 == e.getCode()) {
				// no toplevel ? --> dummy root
				return true;
			} else {
				throw e;
			}
		}
		return false;
	}

	private void addAtgEngine(BtcVectorGenerationStep step, EngineSettings settings) {
		EngineAtg atg = settings.getEngineAtg();
		atg.setSearchDepthSteps(step.getDepthAtg());
		atg.setTimeoutSecondsPerSubsystem(step.getScopeTimeout());
		ExecutionModeEnum executionModeAtg = ExecutionModeEnum.valueOf(step.getExecutionModeAtg());
		atg.setExecutionMode(executionModeAtg);
		settings.setEngineAtg(atg);
	}

	private void addCvEngine(BtcVectorGenerationStep step, EngineSettings settings) {
		EngineCv cv = settings.getEngineCv();
		cv.setSearchDepthSteps(step.getDepthCv());
		cv.timeoutSecondsPerSubsystem(step.getScopeTimeout());
		cv.setTimeoutSecondsPerProperty(step.getPropertyTimeout());
		cv.setLoopUnroll(step.getLoopUnroll());
		// hard coded...
		cv.setSearchFocus(SearchFocusEnum.BALANCED);
		cv.setCoreEngines(Arrays.asList(new CoreEngine().name(NameEnum.ISAT).use(UseEnum.TRUE)));
		// search focus
		// assumption check
		// core engines
		// memory limit
		ParallelExecutionModeEnum parallelExecutionMode = ParallelExecutionModeEnum
				.valueOf(step.getParallelExecutionMode());
		cv.setParallelExecutionMode(parallelExecutionMode);
		cv.setMaximumNumberOfThreads(step.getNumberOfThreads());
		settings.setEngineCv(cv);
	}

	private void prepareAndExecuteVectorGeneration() throws ApiException {
		Config config = vectorGenerationApi.getConfiguration();
		// diable all target definitions -> use pll
//		List<TargetDefinition> targetDefinitions = config.getTargetDefinitions().stream()
//				.map(def -> def.enabled(false))
//				.collect(Collectors.toList());
//		config.setTargetDefinitions(targetDefinitions);
		config.getTargetDefinitions().forEach(def -> def.enabled(EnabledEnum.FALSE));
		config.setPllString(step.getPll());
		config.setCheckUnreachableProperties(step.isRecheckUnreachable() ? CheckUnreachablePropertiesEnum.TRUE : CheckUnreachablePropertiesEnum.FALSE);
		config.setIsSubscopesGoalsConsidered(step.isConsiderSubscopes() ? IsSubscopesGoalsConsideredEnum.TRUE : IsSubscopesGoalsConsideredEnum.FALSE);
		EngineSettings engineSettings = config.getEngineSettings();
		switch (step.getEngine().toUpperCase()) {
		case "ATG+CV":
			addAtgEngine(step, engineSettings);
			addCvEngine(step, engineSettings);
			break;
		case "ATG":
			addAtgEngine(step, engineSettings);
			// disable cv
			engineSettings.getEngineCv().use(EngineCv.UseEnum.FALSE);
			break;
		case "CV":
			addCvEngine(step, engineSettings);
			// disable atg
			engineSettings.getEngineAtg().use(EngineAtg.UseEnum.FALSE);
			break;
		default:
			break;
		}
		engineSettings.setAllowDenormalizedFloats(step.isAllowDenormalizedFloats() ? AllowDenormalizedFloatsEnum.TRUE : AllowDenormalizedFloatsEnum.FALSE);
		engineSettings.setAnalyseSubScopesHierarchically(step.isAnalyzeSubscopesHierarchically() ? AnalyseSubScopesHierarchicallyEnum.TRUE : AnalyseSubScopesHierarchicallyEnum.FALSE);
		engineSettings.setHandlingRateThreshold(step.getHandlingRateThreshold());
		engineSettings.setTimeoutSeconds(step.getGlobalTimeout());
		config.setEngineSettings(engineSettings);

		if (isDummyRoot(scopeApi)) {
			// toplevel s a dummy scope
			List<Scope> scopes = scopeApi.getScopesByQuery1(null, TRUE);
			for (Scope scope : scopes) {
				config.setScope(scope);
				Job vectorGeneration = vectorGenerationApi.execute(config);
				HttpRequester.waitForCompletion(vectorGeneration.getJobID());
			}
		} else {
			// toplevel scope is part of the SUT
			Scope toplevel = Util.getToplevelScope();
			config.setScope(toplevel);
			Job vectorGeneration = vectorGenerationApi.execute(config);
			HttpRequester.waitForCompletion(vectorGeneration.getJobID());
		}

	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcVectorGenerationStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String pll = "::";
	private List<String> goals;
	private String engine = "ATG+CV";
	private String executionModeAtg = ExecutionModeEnum.TOP_DOWN.name();
	private int globalTimeout = 600;
	private int scopeTimeout = 30;
	private int propertyTimeout = 60;
	private boolean considerSubscopes = true;
	private boolean analyzeSubscopesHierarchically = true;
	private boolean allowDenormalizedFloats = true;
	private boolean recheckUnreachable = false;
	private int depthCv = 10;
	private int depthAtg = 20;
	private int loopUnroll = 50;
	private int handlingRateThreshold = 100;
	private int numberOfThreads = 1;
	private String parallelExecutionMode = ParallelExecutionModeEnum.BALANCED.name();
	// search focus
	// assumption check
	// core engines
	// memory limit

	// specific to jenkins
	private boolean robustnessTestFailure = false;
	private boolean createReport = false;
	private boolean showProgress;

	@DataBoundConstructor
	public BtcVectorGenerationStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcVectorGenerationExecution(this, context);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "btcVectorGeneration";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Vector Generation Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getPll() {
		return pll;
	}

	@DataBoundSetter
	public void setPll(String pll) {
		this.pll = pll;
	}

	public String getEngine() {
		return engine;
	}

	@DataBoundSetter
	public void setEngine(String engine) {
		this.engine = engine;
	}

	public String getExecutionModeAtg() {
		return executionModeAtg;

	}

	@DataBoundSetter
	public void setExecutionModeAtg(String executionModeAtg) {
		this.executionModeAtg = executionModeAtg;

	}

	public int getGlobalTimeout() {
		return globalTimeout;
	}

	@DataBoundSetter
	public void setGlobalTimeout(int globalTimeout) {
		this.globalTimeout = globalTimeout;
	}

	public int getScopeTimeout() {
		return scopeTimeout;
	}

	@DataBoundSetter
	public void setScopeTimeout(int scopeTimeout) {
		this.scopeTimeout = scopeTimeout;
	}

	public int getPropertyTimeout() {
		return propertyTimeout;
	}

	@DataBoundSetter
	public void setPropertyTimeout(int propertyTimeout) {
		this.propertyTimeout = propertyTimeout;
	}

	public boolean isConsiderSubscopes() {
		return considerSubscopes;
	}

	@DataBoundSetter
	public void setConsiderSubscopes(boolean considerSubscopes) {
		this.considerSubscopes = considerSubscopes;
	}

	public boolean isAnalyzeSubscopesHierarchically() {
		return analyzeSubscopesHierarchically;
	}

	@DataBoundSetter
	public void setAnalyzeSubscopesHierarchically(boolean analyzeSubscopesHierarchically) {
		this.analyzeSubscopesHierarchically = analyzeSubscopesHierarchically;
	}

	public boolean isAllowDenormalizedFloats() {
		return allowDenormalizedFloats;
	}

	@DataBoundSetter
	public void setAllowDenormalizedFloats(boolean allowDenormalizedFloats) {
		this.allowDenormalizedFloats = allowDenormalizedFloats;
	}

	public boolean isRecheckUnreachable() {
		return recheckUnreachable;
	}

	@DataBoundSetter
	public void setRecheckUnreachable(boolean recheckUnreachable) {
		this.recheckUnreachable = recheckUnreachable;
	}

	public int getDepthCv() {
		return depthCv;
	}

	@DataBoundSetter
	public void setDepthCv(int depthCv) {
		this.depthCv = depthCv;
	}

	public int getDepthAtg() {
		return depthAtg;
	}

	@DataBoundSetter
	public void setDepthAtg(int depthAtg) {
		this.depthAtg = depthAtg;
	}

	public int getLoopUnroll() {
		return loopUnroll;
	}

	@DataBoundSetter
	public void setLoopUnroll(int loopUnroll) {
		this.loopUnroll = loopUnroll;
	}

	public int getHandlingRateThreshold() {
		return handlingRateThreshold;

	}

	@DataBoundSetter
	public void setHandlingRateThreshold(int handlingRateThreshold) {
		this.handlingRateThreshold = handlingRateThreshold;

	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	@DataBoundSetter
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public String getParallelExecutionMode() {
		return parallelExecutionMode;
	}

	@DataBoundSetter
	public void setParallelExecutionMode(String parallelExecutionMode) {
		this.parallelExecutionMode = parallelExecutionMode;
	}

	public boolean isRobustnessTestFailure() {
		return robustnessTestFailure;
	}

	@DataBoundSetter
	public void setRobustnessTestFailure(boolean robustnessTestFailure) {
		this.robustnessTestFailure = robustnessTestFailure;
	}

	public boolean isCreateReport() {
		return createReport;
	}

	@DataBoundSetter
	public void setCreateReport(boolean createReport) {
		this.createReport = createReport;
	}

	public boolean isShowProgress() {
		return showProgress;

	}

	@DataBoundSetter
	public void setShowProgress(boolean showProgress) {
		this.showProgress = showProgress;

	}

	public List<String> getGoals() {
		return goals;
	}

	@DataBoundSetter
	public void setGoals(List<String> goals) {
		this.goals = goals;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
