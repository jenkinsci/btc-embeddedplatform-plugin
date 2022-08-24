package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

import com.btc.ep.plugins.embeddedplatform.http.EPApiClient;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.MetaInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.PilInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.StepArgSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.ProcessHelper;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.btc.ep.plugins.embeddedplatform.util.WindowsHelper;

import hudson.Extension;
import hudson.Proc;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcStartupStepExecution extends SynchronousNonBlockingStepExecution<Object> {

	private static final long serialVersionUID = 1L;
	private BtcStartupStep step;

	public BtcStartupStepExecution(BtcStartupStep step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	public Object run() {
		// startup step init stuff
		JUnitXMLHelper.initialize();
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		StartupExecution exec = new StartupExecution(step, logger, getContext());
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		StepExecutionHelper.postProcessing(stepResult);
		return null;
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
	private String matlabVersion;
	private String licensingPackage = "ET_COMPLETE";
	private String licenseLocationString;
	private String additionalJvmArgs;
	private int timeout = 120;
	private int port = 29267;
	private boolean simplyConnect;
	private boolean skipReportInitialization;
	private String ipAddress;

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

	public String getMatlabVersion() {
		return matlabVersion;
	}

	@DataBoundSetter
	public void setMatlabVersion(String matlabVersion) {
		this.matlabVersion = matlabVersion;
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

class StartupExecution extends BtcExecution {

	private static final long serialVersionUID = -832370371669869980L;

	private BtcStartupStep step;
	private String epVersion = "";

	private transient Proc epProcess;

	public StartupExecution(BtcStartupStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
		initBeforeExecution();
	}

	private void initBeforeExecution() {
		// ...but prepare reporting for successive steps
		Store.startDate = new Date();
		Store.matlabVersion = step.getMatlabVersion();
		if (!step.isSkipReportInitialization()) {
			initializeReporting();
		}
	}

	@Override
	public Object performAction() throws Exception {
		// don't add this step to the report...
		noReporting();
		
		if (step.getIpAddress() != null) {
			step.setPort(8080);
		}
		
		String host = step.getIpAddress() != null ? step.getIpAddress() : "localhost";
		
		log("Connecting to BTC on " + host + ":" + step.getPort() + ". This may take up to a minute...");
			
		checkArgument(host != null, "Cannot resolve agent IP address for remote connection.");
		ApiClient apiClient = new EPApiClient().setBasePath("http://" + host + ":" + step.getPort());
		apiClient.setReadTimeout(10000);
		Configuration.setDefaultApiClient(apiClient);
		HttpRequester.port = step.getPort();
		
		// Connect or startup EP
		boolean connected = HttpRequester.checkConnection("/ep/test", 200, logger());
		if (connected) {
			response(201);
		} else {
			// start command call can be skipped if we only connect to a starting instance
			if (!step.isSimplyConnect() && !isUnix) {
				// prepare ep start command
				List<String> command = createStartCommand();
				// start process and save it for future use (e.g. to destroy it)
				try {
					epProcess = ProcessHelper.spawnManagedProcess(command, /* getContext() */ null);
				} catch (Exception e) {
					error("Failed to start BTC EmbeddedPlatform.", e);
					epProcess.kill();
					return response(400);
				}
			}

			// wait for ep rest service to respond
			connected = HttpRequester.checkConnection("/ep/test", 200, step.getTimeout(), 2);
			if (connected) {
				log("Successfully connected to BTC EmbeddedPlatform " + epVersion + " on port " + step.getPort());
				response(200);
			} else {
				error("Connection timed out after " + step.getTimeout() + " seconds.");
				failed();
				return response(400);
			}
			logger().println((connected ? "Successfully" : "Not") +  " connected to BTC EmbeddedPlatform.");
		}
		return getResponse();
		
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
		String osName = System.getProperty("os.name").toLowerCase();
		logger().println("os.name: " + osName);
		if (osName.contains("win")) {
			// prepare data for process call
			String epInstallPath = getEpInstallPath().replaceAll("\\\\", "/");
			epVersion = epInstallPath.substring(epInstallPath.lastIndexOf("/") + 1).substring(2);
			logger().println("Starting BTC EmbeddedPlatform on Windows...");
			return createStartCommand_WINDOWS(epVersion, epInstallPath, licensingPackage, port);
		} else {
			logger().println("Starting BTC EmbeddedPlatform on Linux...");
			return createStartCommand_LINUX(licensingPackage);
		}
	}

	private String getEpInstallPath() throws Exception {
		if (step.getInstallPath() != null) {
			return step.getInstallPath();
		} else {
			return WindowsHelper.getEpInstallPathFromRegistry();
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
		
		/*
		 * Try to find the jre/jdk and add the bin folder to the path
		 * Effect: only one process (ep.exe) is started, not ep.exe + javaw.exe
		 */
		addJreString(epInstallPath, command);
		
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
		} else {
			command.add("-Xmx1g");
		}
		return command;
	}
	
	/**
	 * Tries to find the jdk/jre that is shipped with BTC EmbeddedPlatform. If successful, adds the bin folder explicitly as -vm
	 * 
	 * @param epInstallPath the ep install dir
	 * @param command the command to add to
	 */
	private void addJreString(String epInstallPath, List<String> command) {
		try {
			File jreParentDir = new File(epInstallPath + "/jres");
			checkArgument(jreParentDir != null, "Failed to find the Java directory in the EP installation ('" + jreParentDir + "')");
			File[] subdirs = jreParentDir.listFiles(f -> f.isDirectory());
			String jreBinPathParent = null;
			for (File subdir : subdirs) {
				if (subdir.getName().startsWith("jdk")) {
					jreBinPathParent = subdir.getAbsolutePath();
					break;
				}
			}
			String jreBinPath = jreBinPathParent + "/bin";
			command.add("-vm");
			command.add("\"" + jreBinPath + "\"");
		} catch (Exception e) {
//			warning("Could not determine proper jdk path for btc-embeddedplatform.");
		}
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
		command.add("sh"); // path to ep is fixed based on docker image
		command.add("-c");
		StringBuilder sb = new StringBuilder();
		sb.append("\\\"${EP_INSTALL_PATH}/ep"); // path to ep is fixed based on docker image
		sb.append(" -clearPersistedState");
		sb.append(" -application");
		sb.append(" ep.application.headless");
		sb.append(" -nosplash");
		sb.append(" -vmargs");
		sb.append(" -Dep.runtime.batch=ep");
		sb.append(" -Dep.linux.config=\\${EP_REGISTRY}");
		sb.append(" -Dlogback.configurationFile=\\${EP_LOG_CONFIG}");
		sb.append(" -Dep.configuration.logpath=\\${WORKSPACE}/logs");
		sb.append(" -Dep.runtime.workdir=\\${WORK_DIR}");
		sb.append(" -Dct.root.temp.dir=\\${TMP_DIR}");
		sb.append(" -Dep.licensing.location=\\${LICENSE_LOCATION}");
		sb.append(" -Dep.licensing.package=" + licensingPackage);
		sb.append(" -Dep.rest.port=${REST_PORT}");
		if (step.getAdditionalJvmArgs() != null) {
			sb.append(" ").append(step.getAdditionalJvmArgs());
		}
		sb.append("\\\"");
		command.add(sb.toString());
		return command;
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

}
