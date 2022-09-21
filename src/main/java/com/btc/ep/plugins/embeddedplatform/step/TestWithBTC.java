package com.btc.ep.plugins.embeddedplatform.step;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import com.btc.ep.plugins.embeddedplatform.exception.ErrorOccurredException;
import com.btc.ep.plugins.embeddedplatform.model.TestConfig;
import com.btc.ep.plugins.embeddedplatform.reporting.project.BasicStep;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcAddDomainCheckGoals;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcB2BStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcCodeAnalysisReportStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcRBTStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcVectorGenerationStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateCStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateTLStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcExecutionRecordExportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcExecutionRecordImportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcInputRestrictionsExportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcInputRestrictionsImportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcToleranceExportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcToleranceImportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcToleranceResetStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorImportStep;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class TestWithBTCStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private TestWithBTC step;

	public TestWithBTCStepExecution(TestWithBTC step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Object run() throws Exception {
		PrintStream logger = getContext().get(TaskListener.class).getLogger();
		// Load test config
		StepExecutionHelper.log(logger, "Applying specified test config file " + step.getTestConfigPath());
		FilePath testConfigFilePath = StepExecutionHelper.resolveInAgentWorkspace(getContext(), step.getTestConfigPath());
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
		TestConfig testConfig = yaml.loadAs(testConfigFilePath.read(), TestConfig.class);
		// apply specifically, because may be determined via a groovy step and then passed to the step
		testConfig.generalOptions.setIpAddress(step.getIpAddress());
		
		// Initial sanity checks
		checkTestConfig(testConfig);

		// Startup / Connect
		run(testConfig.generalOptions, new BtcStartupStep());

		// Run test steps
		runSteps(testConfig.testSteps, logger);

		// Wrap up
		new BtcWrapUpStep().start(getContext()).start();
		return null;
	}

	private void runSteps(List<Map<String, Object>> testSteps, PrintStream logger) {
		boolean skipRemainingStepsDueToFailure = false;
		for (Map<String, Object> testStep : testSteps) {
			if (!skipRemainingStepsDueToFailure) {
				try {
					runStep(testStep, logger);
				} catch (Exception e) {
					StepExecutionHelper.log(logger, "Skipping remaining steps due to failure.");
					skipRemainingStepsDueToFailure = true;
				}
			} else {
				markStepAsSkipped(testStep);
			}
		}
	}

	protected void markStepAsSkipped(Map<String, Object> testStep) {
		String capitalizedStepName = StringUtils.capitalize(testStep.get("name").toString());
		BasicStep reportingStep = new TestStep(capitalizedStepName);
		reportingStep.setInfo("Skipped due to earlier failure");
		reportingStep.setStatusOK(false);
		reportingStep.setStatusWARNING(false);
		reportingStep.setSkipped(true);
		Store.testStepSection.addStep(reportingStep);
	}

	/**
	 * Grabs the value from the testStep objects, applies them to the target step
	 * and starts it. Ensures that mandatory values are present.
	 * 
	 * @param testStep the input data
	 */
	private void runStep(Map<String, Object> testStep, PrintStream logger) throws Exception {
		String stepName = (String) testStep.get("name");
		switch (stepName) {
		/*
		 * Initial Steps
		 */
		case "loadProfile":
			String profilePath = (String) testStep.get("profilePath");
			checkArgument(profilePath != null, "No profilePath was provided for the Profile Load step.");
			run(testStep, new BtcProfileLoadStep(profilePath));
			break;
		case "cCode":
			String codeModelPath = (String) testStep.get("codeModelPath");
			checkArgument(codeModelPath != null, "No codeModelPath was provided for the C-Code Profile Creation step.");
			run(testStep, new BtcProfileCreateCStep(codeModelPath));
			break;
		case "targetLink":
			String tlModelPath = (String) testStep.get("tlModelPath");
			checkArgument(tlModelPath != null, "No tlModelPath was provided for the TargetLink Profile Creation step.");
			run(testStep, new BtcProfileCreateTLStep(tlModelPath));
			break;
		case "embeddedCoder":
			String slModelPath1 = (String) testStep.get("slModelPath");
			checkArgument(slModelPath1 != null,
					"No slModelPath was provided for the EmbeddedCoder Profile Creation step.");
			run(testStep, new BtcProfileCreateTLStep(slModelPath1));
			break;
		case "simulink":
			String slModelPath2 = (String) testStep.get("slModelPath");
			checkArgument(slModelPath2 != null, "No slModelPath was provided for the Simulink Profile Creation step.");
			run(testStep, new BtcProfileCreateTLStep(slModelPath2));
			break;
		case "simulinkToplevel":
			String slModelPath3 = (String) testStep.get("slModelPath");
			checkArgument(slModelPath3 != null, "No slModelPath was provided for the Simulink Profile Creation step.");
			run(testStep, new BtcProfileCreateTLStep(slModelPath3));
			break;

		/*
		 * Analysis Steps
		 */
		case "requirementsBasedTest":
			run(testStep, new BtcRBTStep());
			break;
		case "backToBackTest":
			run(testStep, new BtcB2BStep());
			break;
		case "vectorGeneration":
			run(testStep, new BtcVectorGenerationStep());
			break;
		case "codeAnalysisReport":
			run(testStep, new BtcCodeAnalysisReportStep());
			break;
		case "domainCheckGoals":
			run(testStep, new BtcAddDomainCheckGoals());
			break;

		/*
		 * Import & Export
		 */
		case "executionRecordImport":
			String erImportDir = (String) testStep.get("dir");
			checkArgument(erImportDir != null, "No directory was provided for the Execution Record Import step");
			run(testStep, new BtcExecutionRecordImportStep(erImportDir));
			break;
		case "executionRecordExport":
			run(testStep, new BtcExecutionRecordExportStep());
			break;
		case "inputRestrictionsImport":
			String irImportDir = (String) testStep.get("path");
			checkArgument(irImportDir != null, "No path was provided for the Input Restrictions Import step");
			run(testStep, new BtcInputRestrictionsImportStep(irImportDir));
			break;
		case "inputRestrictionsExport":
			run(testStep, new BtcInputRestrictionsExportStep());
			break;
		case "toleranceImport":
			String tolImportDir = (String) testStep.get("path");
			checkArgument(tolImportDir != null, "No path was provided for the Tolerance Import step");
			run(testStep, new BtcToleranceImportStep(tolImportDir));
			break;
		case "toleranceExport":
			run(testStep, new BtcToleranceExportStep());
			break;
		case "toleranceReset":
			run(testStep, new BtcToleranceResetStep());
			break;
		case "vectorImport":
			String vectorImportDir = (String) testStep.get("importDir");
			checkArgument(vectorImportDir != null, "No directory was provided for the Vector Import step");
			run(testStep, new BtcVectorImportStep(vectorImportDir));
			break;
//		case "vectorExport":
//			run(testStep, new BtcVectorExportStep());
//			break;

		case "custom":
			// special step to invoke plugins, etc.
			checkArgument(testStep.get("apiPath") != null, "No apiPath was provided");
			run(testStep, new BtcVectorImportStep(null));
			break;
		default:
			StepExecutionHelper.log(logger, "Test Step '%s' is not a supported step. Please refer to the docs and verify the spelling.", stepName);
			break;
		}

	}

	/**
	 * Applies the sourceObjects values to the target step and runs it.
	 * 
	 * @param sourceObject the source object with matching attributes /
	 *                     getter/setters
	 * @param targetStep   the step to be executed
	 */
	private void run(Object sourceObject, Step targetStep) throws Exception {
		StepExecution stepExecution = Util.applyMatchingFields(sourceObject, targetStep).start(getContext());
		stepExecution.start();
		// stepExecution.start() will not throw any exception from the step execution
		// parse step execution's status
		if (Status.ERROR.toString().equals(stepExecution.getStatus())) {
			throw new ErrorOccurredException();
		}
	}

	private void checkTestConfig(TestConfig testConfig) {
		// check if config is available and has steps
		checkArgument(testConfig != null, "No valid TestConfig was provided.");
		checkArgument(testConfig.testSteps != null && !testConfig.testSteps.isEmpty(),
				"Provided TestConfig " + step.getTestConfigPath() + " does not contain any test steps.");

		// check if the config has exactly 1 init step
		List<Object> initSteps = testConfig.testSteps.stream()
				.filter(step -> TestConfig.INIT_STEPS.contains(step.get("name"))).map(step -> step.get("name"))
				.collect(Collectors.toList());
		int numberOfInitSteps = initSteps.size();
		checkArgument(numberOfInitSteps != 0, "TestConfig " + step.getTestConfigPath()
				+ " must include one of the available initial steps: " + TestConfig.INIT_STEPS);
		checkArgument(numberOfInitSteps == 1, "TestConfig " + step.getTestConfigPath()
				+ " must not include more than initial steps. Found: " + initSteps);

	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class TestWithBTC extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String testConfigPath = "TestConfig.yaml";
	private String ipAddress;

	@DataBoundConstructor
	public TestWithBTC() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new TestWithBTCStepExecution(this, context);
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
			return "TestWithBTC";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Tests using testconfig";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getTestConfigPath() {
		return testConfigPath;
	}

	@DataBoundSetter
	public void setTestConfigPath(String testConfigPath) {
		this.testConfigPath = testConfigPath;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	@DataBoundSetter
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
