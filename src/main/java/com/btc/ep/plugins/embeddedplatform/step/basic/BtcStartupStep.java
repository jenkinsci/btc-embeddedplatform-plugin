package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

import com.btc.ep.plugins.embeddedplatform.http.EPApiClient;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.MetaInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.PilInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.StepArgSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;
import com.btc.ep.plugins.embeddedplatform.util.ProcessHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.btc.ep.plugins.embeddedplatform.util.WindowsHelper;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcStartupStepExecution extends AbstractBtcStepExecution {

	private static final long serialVersionUID = 1L;
	private BtcStartupStep step;
	private String epVersion = "";

	public BtcStartupStepExecution(BtcStartupStep step, StepContext context) {
		super(step, context);
		this.step = step;
	}

	@Override
	protected void performAction() throws Exception {
		// don't add this step to the report...
		noReporting();
		// ...but prepare reporting for successive steps
		Store.startDate = new Date();
		if (!step.isSkipReportInitialization()) {
			initializeReporting();
		}

		// Prepare http connection
		ApiClient apiClient = new EPApiClient().setBasePath("http://localhost:" + step.getPort());
		apiClient.setReadTimeout(10000);
		Configuration.setDefaultApiClient(apiClient);
		HttpRequester.port = step.getPort();
		
		// Connect or startup EP
		boolean connected = HttpRequester.checkConnection("/ep/test", 200, jenkinsConsole);
		if (connected) {
			response = 201;
		} else {
			// start command call can be skipped if we only connect to a starting instance
			if (!step.isSimplyConnect()) {
				// prepare ep start command
				List<String> command = createStartCommand();
				// start process and save it for future use (e.g. to destroy it)
				try {
					log("Starting BTC EmbeddedPlatform: " + String.join(" ", command));
					Store.epProcess = ProcessHelper.spawnManagedProcess(command, getContext());
				} catch (Exception e) {
					error("Failed to start BTC EmbeddedPlatform.", e);
					Store.epProcess.kill();
					return;
				}
			}

			// wait for ep rest service to respond
			connected = HttpRequester.checkConnection("/ep/test", 200, step.getTimeout(), 2, jenkinsConsole);
			if (connected) {
				log("Successfully connected to BTC EmbeddedPlatform " + epVersion + " on port " + step.getPort());
				response = 200;
			} else {
				error("Connection timed out after " + step.getTimeout() + " seconds.");
				failed();
				response = 400;
				return;
			}
		}
	}

	/**
	 * Initializes the reporting
	 */
	private void initializeReporting() {
		Store.reportData = new JenkinsAutomationReport();
		String startDateString = Util.DATE_FORMAT.format(Store.startDate);
		Store.reportData.setStartDate(startDateString);
		Store.testStepSection = new TestStepSection();
		Store.pilInfoSection = new PilInfoSection();
		Store.metaInfoSection = new MetaInfoSection();
		Store.testStepArgumentSection = new StepArgSection();
	}

	/**
	 * Creates the ep start command
	 *
	 * @return the ep start command
	 * @throws IOException path issues...
	 */
	private List<String> createStartCommand() throws Exception {
		String licensingPackage = step.getLicensingPackage();
		int port = step.getPort();
		/*
		 * IMPORTANT: Don't use something like System.getProperty("os.name"), it will
		 * return the operating system of the Jenkins controller
		 */
		if (isUnix()) {
			return createStartCommand_LINUX(licensingPackage);
		} else {
			// prepare data for process call
			String epInstallPath = getEpInstallPath().replaceAll("\\\\", "/");
			epVersion = epInstallPath.substring(epInstallPath.lastIndexOf("/") + 1).substring(2);
			return createStartCommand_WINDOWS(epVersion, epInstallPath, licensingPackage, port);
		}
	}

	private String getEpInstallPath() throws Exception {
		if (step.getInstallPath() != null) {
			return step.getInstallPath();
		} else {
			return WindowsHelper.getEpInstallPathFromRegistry(getContext());
		}
	}

	/**
	 * Creates the ep start command for windows
	 *
	 * @param epVersion        ep version string
	 * @param epInstallPath    epInstallPath
	 * @param licensingPackage license package
	 * @param port             the port to use
	 * @return the ep start command
	 * @throws IOException path issues...
	 */
	private List<String> createStartCommand_WINDOWS(String epVersion, String epInstallPath, String licensingPackage,
			int port) throws IOException {
		String epExecutable = epInstallPath + "/rcp/ep.exe";
		List<String> command = new ArrayList<>();
		command.add("\"" + epExecutable + "\"");
		command.add("-clearPersistedState");
		command.add("-application");
		command.add("ep.application.headless");
		command.add("-nosplash");
		command.add("-vmargs");
		command.add("-Dep.runtime.batch=ep");
		command.add("-Dosgi.configuration.area.default=@user.home/AppData/Roaming/BTC/ep/" + epVersion + "/" + port
				+ "/configuration");
		command.add("-Dosgi.instance.area.default=@user.home/AppData/Roaming/BTC/ep/" + epVersion + "/" + port
				+ "/workspace");
		command.add("-Dep.configuration.logpath=AppData/Roaming/BTC/ep/" + epVersion + "/" + port + "/logs");
		command.add("-Dep.runtime.workdir=BTC/ep/" + epVersion + "/" + port);
		command.add("-Dep.licensing.package=" + licensingPackage);
		command.add("-Dep.rest.port=" + port);
		if (step.getAdditionalJvmArgs() != null) {
			command.add(step.getAdditionalJvmArgs());
		}
		return command;
	}

	/**
	 * Creates the ep start command for linux (docker)
	 *
	 * @param licensingPackage license package
	 * @return the ep start command
	 * @throws IOException path issues...
	 * 
	 *	["/opt/Export/ep", "-clearPersistedState",
	 *	"-application", "ep.application.headless", "-nosplash",
	 *	"-console", "-consoleLog", "-vmargs",
	 * 	"-Dep.linux.config=/opt/.eplinuxregistry",
	 *  "-Dep.licensing.package=EP_FULL"]
	 */
	private List<String> createStartCommand_LINUX(String licensingPackage) throws IOException {
		List<String> command = new ArrayList<>();
		command.add("/opt/Export/ep"); // path to ep is fixed based on docker image
		command.add("-clearPersistedState");
		command.add("-application");
		command.add("ep.application.headless");
		command.add("-nosplash");
		command.add("-vmargs");
		command.add("-Dep.runtime.batch=ep");
		command.add("-Dep.linux.config=/opt/.eplinuxregistry");
		command.add("-Dep.licensing.package=" + licensingPackage);
		if (step.getAdditionalJvmArgs() != null) {
			command.add(step.getAdditionalJvmArgs());
		}
		return command;
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcStartupStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String installPath;
	private String licensingPackage = "ET_COMPLETE";
	private String licenseLocationString;
	private String additionalJvmArgs;
	private int timeout = 120;
	private int port = 29267;
	private boolean simplyConnect;
	private boolean skipReportInitialization;

	@DataBoundConstructor
	public BtcStartupStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcStartupStepExecution(this, context);
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
			return "btcStart";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC EmbeddedPlatform Start up";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	public String getInstallPath() {
		return installPath;
	}

	@DataBoundSetter
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	public boolean isSimplyConnect() {
		return simplyConnect;
	}

	@DataBoundSetter
	public void setSimplyConnect(boolean simplyConnect) {
		this.simplyConnect = simplyConnect;
	}

	public int getPort() {
		return port;

	}

	@DataBoundSetter
	public void setPort(int port) {
		this.port = port;

	}

	public String getLicensingPackage() {
		return licensingPackage;

	}

	@DataBoundSetter
	public void setLicensingPackage(String licensingPackage) {
		this.licensingPackage = licensingPackage;

	}

	public String getAdditionalJvmArgs() {
		return additionalJvmArgs;

	}

	@DataBoundSetter
	public void setAdditionalJvmArgs(String additionalJvmArgs) {
		this.additionalJvmArgs = additionalJvmArgs;

	}

	public int getTimeout() {
		return timeout;

	}

	@DataBoundSetter
	public void setTimeout(int timeout) {
		this.timeout = timeout;

	}

	public String getLicenseLocationString() {
		return licenseLocationString;

	}

	@DataBoundSetter
	public void setLicenseLocationString(String licenseLocationString) {
		this.licenseLocationString = licenseLocationString;

	}

	public boolean isSkipReportInitialization() {
		return skipReportInitialization;
	}

	@DataBoundSetter
	public void setSkipReportInitialization(boolean skipReportInitialization) {
		this.skipReportInitialization = skipReportInitialization;
	}

	/*
	 * End of getter/setter section
	 */

} // end of step class
