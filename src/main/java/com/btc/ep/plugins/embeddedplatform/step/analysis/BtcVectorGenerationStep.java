package com.btc.ep.plugins.embeddedplatform.step.analysis;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
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
import org.openapitools.client.api.CoverageGenerationApi;
import org.openapitools.client.api.ReportsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Config;
import org.openapitools.client.model.CoreEngine;
import org.openapitools.client.model.EngineAtg;
import org.openapitools.client.model.EngineAtg.ExecutionModeEnum;
import org.openapitools.client.model.EngineCv;
import org.openapitools.client.model.EngineCv.ParallelExecutionModeEnum;
import org.openapitools.client.model.EngineCv.SearchFocusEnum;
import org.openapitools.client.model.EngineSettings;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Report;
import org.openapitools.client.model.ReportExportInfo;
import org.openapitools.client.model.Scope;

import com.btc.ep.plugins.embeddedplatform.http.GenericResponse;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.util.BtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.google.gson.Gson;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcVectorGenerationStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String pll = "::";
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

        /*
         * This specifies the step name that the the user can use in his Jenkins Pipeline
         * - for example: btcStartup installPath: 'C:/Program Files/BTC/ep2.9p0', port: 29267
         */
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
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /**
     * Get pll.
     * 
     * @return the pll
     */
    public String getPll() {
        return pll;
    }

    /**
     * Set pll.
     * 
     * @param pll the pll to set
     */
    @DataBoundSetter
    public void setPll(String pll) {
        this.pll = pll;
    }

    /**
     * Get engine.
     * 
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Set engine.
     * 
     * @param engine the engine to set
     */
    @DataBoundSetter
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Get executionModeAtg.
     * 
     * @return the executionModeAtg
     */
    public String getExecutionModeAtg() {
        return executionModeAtg;

    }

    /**
     * Set executionModeAtg.
     * 
     * @param executionModeAtg the executionModeAtg to set
     */
    @DataBoundSetter
    public void setExecutionModeAtg(String executionModeAtg) {
        this.executionModeAtg = executionModeAtg;

    }

    /**
     * Get globalTimeout.
     * 
     * @return the globalTimeout
     */
    public int getGlobalTimeout() {
        return globalTimeout;
    }

    /**
     * Set globalTimeout.
     * 
     * @param globalTimeout the globalTimeout to set
     */
    @DataBoundSetter
    public void setGlobalTimeout(int globalTimeout) {
        this.globalTimeout = globalTimeout;
    }

    /**
     * Get scopeTimeout.
     * 
     * @return the scopeTimeout
     */
    public int getScopeTimeout() {
        return scopeTimeout;
    }

    /**
     * Set scopeTimeout.
     * 
     * @param scopeTimeout the scopeTimeout to set
     */
    @DataBoundSetter
    public void setScopeTimeout(int scopeTimeout) {
        this.scopeTimeout = scopeTimeout;
    }

    /**
     * Get propertyTimeout.
     * 
     * @return the propertyTimeout
     */
    public int getPropertyTimeout() {
        return propertyTimeout;
    }

    /**
     * Set propertyTimeout.
     * 
     * @param propertyTimeout the propertyTimeout to set
     */
    @DataBoundSetter
    public void setPropertyTimeout(int propertyTimeout) {
        this.propertyTimeout = propertyTimeout;
    }

    /**
     * Get considerSubscopes.
     * 
     * @return the considerSubscopes
     */
    public boolean isConsiderSubscopes() {
        return considerSubscopes;
    }

    /**
     * Set considerSubscopes.
     * 
     * @param considerSubscopes the considerSubscopes to set
     */
    @DataBoundSetter
    public void setConsiderSubscopes(boolean considerSubscopes) {
        this.considerSubscopes = considerSubscopes;
    }

    /**
     * Get analyzeSubscopesHierarchically.
     * 
     * @return the analyzeSubscopesHierarchically
     */
    public boolean isAnalyzeSubscopesHierarchically() {
        return analyzeSubscopesHierarchically;
    }

    /**
     * Set analyzeSubscopesHierarchically.
     * 
     * @param analyzeSubscopesHierarchically the analyzeSubscopesHierarchically to set
     */
    @DataBoundSetter
    public void setAnalyzeSubscopesHierarchically(boolean analyzeSubscopesHierarchically) {
        this.analyzeSubscopesHierarchically = analyzeSubscopesHierarchically;
    }

    /**
     * Get allowDenormalizedFloats.
     * 
     * @return the allowDenormalizedFloats
     */
    public boolean isAllowDenormalizedFloats() {
        return allowDenormalizedFloats;
    }

    /**
     * Set allowDenormalizedFloats.
     * 
     * @param allowDenormalizedFloats the allowDenormalizedFloats to set
     */
    @DataBoundSetter
    public void setAllowDenormalizedFloats(boolean allowDenormalizedFloats) {
        this.allowDenormalizedFloats = allowDenormalizedFloats;
    }

    /**
     * Get recheckUnreachable.
     * 
     * @return the recheckUnreachable
     */
    public boolean isRecheckUnreachable() {
        return recheckUnreachable;
    }

    /**
     * Set recheckUnreachable.
     * 
     * @param recheckUnreachable the recheckUnreachable to set
     */
    @DataBoundSetter
    public void setRecheckUnreachable(boolean recheckUnreachable) {
        this.recheckUnreachable = recheckUnreachable;
    }

    /**
     * Get depthCv.
     * 
     * @return the depthCv
     */
    public int getDepthCv() {
        return depthCv;
    }

    /**
     * Set depthCv.
     * 
     * @param depthCv the depthCv to set
     */
    @DataBoundSetter
    public void setDepthCv(int depthCv) {
        this.depthCv = depthCv;
    }

    /**
     * Get depthAtg.
     * 
     * @return the depthAtg
     */
    public int getDepthAtg() {
        return depthAtg;
    }

    /**
     * Set depthAtg.
     * 
     * @param depthAtg the depthAtg to set
     */
    @DataBoundSetter
    public void setDepthAtg(int depthAtg) {
        this.depthAtg = depthAtg;
    }

    /**
     * Get loopUnroll.
     * 
     * @return the loopUnroll
     */
    public int getLoopUnroll() {
        return loopUnroll;
    }

    /**
     * Set loopUnroll.
     * 
     * @param loopUnroll the loopUnroll to set
     */
    @DataBoundSetter
    public void setLoopUnroll(int loopUnroll) {
        this.loopUnroll = loopUnroll;
    }

    /**
     * Get handlingRateThreshold.
     * 
     * @return the handlingRateThreshold
     */
    public int getHandlingRateThreshold() {
        return handlingRateThreshold;

    }

    /**
     * Set handlingRateThreshold.
     * 
     * @param handlingRateThreshold the handlingRateThreshold to set
     */
    @DataBoundSetter
    public void setHandlingRateThreshold(int handlingRateThreshold) {
        this.handlingRateThreshold = handlingRateThreshold;

    }

    /**
     * Get numberOfThreads.
     * 
     * @return the numberOfThreads
     */
    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    /**
     * Set numberOfThreads.
     * 
     * @param numberOfThreads the numberOfThreads to set
     */
    @DataBoundSetter
    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    /**
     * Get parallelExecutionMode.
     * 
     * @return the parallelExecutionMode
     */
    public String getParallelExecutionMode() {
        return parallelExecutionMode;
    }

    /**
     * Set parallelExecutionMode.
     * 
     * @param parallelExecutionMode the parallelExecutionMode to set
     */
    @DataBoundSetter
    public void setParallelExecutionMode(String parallelExecutionMode) {
        this.parallelExecutionMode = parallelExecutionMode;
    }

    /**
     * Get robustnessTestFailure.
     * 
     * @return the robustnessTestFailure
     */
    public boolean isRobustnessTestFailure() {
        return robustnessTestFailure;
    }

    /**
     * Set robustnessTestFailure.
     * 
     * @param robustnessTestFailure the robustnessTestFailure to set
     */
    @DataBoundSetter
    public void setRobustnessTestFailure(boolean robustnessTestFailure) {
        this.robustnessTestFailure = robustnessTestFailure;
    }

    /**
     * Get createReport.
     * 
     * @return the createReport
     */
    public boolean isCreateReport() {
        return createReport;
    }

    /**
     * Set createReport.
     * 
     * @param createReport the createReport to set
     */
    @DataBoundSetter
    public void setCreateReport(boolean createReport) {
        this.createReport = createReport;
    }

    /**
     * Get showProgress.
     * 
     * @return the showProgress
     */
    public boolean isShowProgress() {
        return showProgress;

    }

    /**
     * Set showProgress.
     * 
     * @param showProgress the showProgress to set
     */
    @DataBoundSetter
    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorGenerationExecution extends BtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcVectorGenerationStep step;

    public BtcVectorGenerationExecution(BtcVectorGenerationStep btcStartupStep, StepContext context) {
        super(btcStartupStep, context);
        this.step = btcStartupStep;
    }

    private CoverageGenerationApi vectorGenerationApi = new CoverageGenerationApi();
    private ScopesApi scopeApi = new ScopesApi();
    private ReportsApi reportApi = new ReportsApi();
    private CodeAnalysisReportsB2BApi b2bCodeAnalysisReportApi = new CodeAnalysisReportsB2BApi();

    @Override
    protected void performAction() throws Exception {
        // only vector generation with default settings is supported until EP-2401 is fixed
        GenericResponse httpResponse = HttpRequester.get("/ep/coverage-generation");
        httpResponse = HttpRequester.post("/ep/coverage-generation", httpResponse.getContent());
        Job job = new Gson().fromJson(httpResponse.getContent(), Job.class);
        HttpRequester.waitForCompletion(job.getJobID());
        String msg = "Successfully executed vectorGeneration";
        // Reporting
        if (step.isCreateReport()) {
            Scope toplevel = scopeApi.getScopesByQuery1(null, true).get(0);
            Report report = b2bCodeAnalysisReportApi.createCodeAnalysisReportOnScope(toplevel.getUid());
            ReportExportInfo info = new ReportExportInfo();
            info.setNewName("CodeCoverageReport");
            String path = new File(Paths.get(getContext().get(FilePath.class).toURI()).toString(), Store.reportPath)
                .getCanonicalPath();
            info.setExportPath(path);
            reportApi.exportReport(report.getUid(), info);
            msg += " and exported the coverage report";
        }
        jenkinsConsole.println(msg);
        response = 200;
    }

    private boolean isDummyRoot(ScopesApi scopeApi) throws ApiException {
        try {
            scopeApi.getScopesByQuery1(null, true);
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
        // EngineAtg atg = settings.getEngineAtg(); // fails due to EP-2401
        EngineAtg atg = new EngineAtg().name("ATG").use(true); // workaround for EP-2401
        atg.setSearchDepthSteps(step.getDepthAtg());
        atg.setTimeoutSecondsPerSubsystem(step.getScopeTimeout());
        ExecutionModeEnum executionModeAtg = ExecutionModeEnum.valueOf(step.getExecutionModeAtg());
        atg.setExecutionMode(executionModeAtg);
        settings.setEngineAtg(atg);
    }

    private void addCvEngine(BtcVectorGenerationStep step, EngineSettings settings) {
        //EngineCv cv = settings.getEngineCv(); // fails due to EP-2401
        EngineCv cv = new EngineCv().name("CV").use(true); // workaround for EP-2401
        cv.setSearchDepthSteps(step.getDepthCv());
        cv.timeoutSecondsPerSubsystem(step.getScopeTimeout());
        cv.setTimeoutSecondsPerProperty(step.getPropertyTimeout());
        cv.setLoopUnroll(step.getLoopUnroll());
        // hard coded...
        cv.setSearchFocus(SearchFocusEnum.BALANCED);
        cv.setCoreEngines(Arrays.asList(new CoreEngine().name("ISAT").use(true)));
        // search focus
        // assumption check
        // core engines
        // memory limit
        ParallelExecutionModeEnum parallelExecutionMode =
            ParallelExecutionModeEnum.valueOf(step.getParallelExecutionMode());
        cv.setParallelExecutionMode(parallelExecutionMode);
        cv.setMaximumNumberOfThreads(step.getNumberOfThreads());
        settings.setEngineCv(cv);
    }

    private void waitingForEP2401() throws ApiException {
        //Config config = vectorGenerationApi.getConfiguration(); // fails due to EP-2401
        Config config = new Config(); // workaround for EP-2401
        config.setPllString(step.getPll());
        config.setCheckUnreachableProperties(step.isRecheckUnreachable());
        config.setIsSubscopesGoalsConsidered(step.isConsiderSubscopes());
        //EngineSettings engineSettings = config.getEngineSettings(); // empty due to workaround for EP-2401
        EngineSettings engineSettings = new EngineSettings(); // workaround for EP-2401
        switch (step.getEngine().toUpperCase()) {
            case "ATG+CV":
                addAtgEngine(step, engineSettings);
                addCvEngine(step, engineSettings);
                break;
            case "ATG":
                addAtgEngine(step, engineSettings);
                // disable cv
                engineSettings.setEngineCv(new EngineCv().use(false));
                break;
            case "CV":
                addCvEngine(step, engineSettings);
                // disable atg
                engineSettings.setEngineAtg(new EngineAtg().use(false));
                break;
            default:
                break;
        }
        engineSettings.setAllowDenormalizedFloats(step.isAllowDenormalizedFloats());
        engineSettings.setAnalyseSubScopesHierarchically(step.isAnalyzeSubscopesHierarchically());
        engineSettings.setHandlingRateThreshold(step.getHandlingRateThreshold());
        engineSettings.setTimeoutSeconds(step.getGlobalTimeout());
        config.setEngineSettings(engineSettings);

        if (isDummyRoot(scopeApi)) {
            // toplevel s a dummy scope
            List<Scope> scopes = scopeApi.getScopesByQuery1(null, false);
            int i = 1;
            for (Scope scope : scopes) {
                config.setScope(scope);
                Job vectorGeneration = vectorGenerationApi.execute(config);
                HttpRequester.waitForCompletion(vectorGeneration.getJobID());
            }
        } else {
            // toplevel scope is part of the SUT
            Scope toplevel = scopeApi.getScopesByQuery1(null, true).get(0);
            config.setScope(toplevel);
            Job vectorGeneration = vectorGenerationApi.execute(config);
            HttpRequester.waitForCompletion(vectorGeneration.getJobID());
        }

    }

}
