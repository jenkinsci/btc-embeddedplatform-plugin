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
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;
import com.btc.ep.plugins.embeddedplatform.util.WindowsHelper;

import hudson.Extension;
import hudson.model.Computer;
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
		
		ConnectionAttempt connectionAttempt = new ConnectionAttempt(step, logger, getContext());
		DataTransferObject response = StepExecutionHelper.executeOnAgent(connectionAttempt, getContext());
		
		// if not connected and on windows -> start EP
		if (!Status.OK.equals(response.status)) {
			startupEpOnWindows(logger, response);
			WaitForConnection exec = new WaitForConnection(step, logger, getContext());
			StepExecutionHelper.executeOnAgent(exec, getContext());
		}
		return null;
	}

	private void startupEpOnWindows(PrintStream logger, DataTransferObject response) {
		if (!step.isSimplyConnect()) {
			try {
				// prepare ep start command
				List<String> command = createStartCommand(logger, isUnix(getContext()));
				// start process and save it for future use (e.g. to destroy it)
				Store.epProcess = ProcessHelper.spawnManagedProcess(command, getContext());
			} catch (Exception e) {
				logger.println("Failed to start BTC EmbeddedPlatform: " + e.getMessage());
				if (Store.epProcess != null) {
					try { Store.epProcess.kill(); } catch (Exception ignored) {}
				}
			}
		}
	}
	
	/**
	 * Creates the ep start command
	 * @param logger 
	 *
	 * @return the ep start command
	 * @throws IOException path issues...
	 */
	private List<String> createStartCommand(PrintStream logger, boolean isUnix) throws Exception {
		String licensingPackage = step.getLicensingPackage();
		int port = step.getPort();
		if (isUnix) {
			logger.println("Starting BTC EmbeddedPlatform on Linux is not implemented. Please call btc.startup() in your pipeline script to start the executable inside of the container.");
			return null;
		} else {
			// prepare data for process call
			String epInstallPath = getEpInstallPath().replaceAll("\\\\", "/");
			String epVersion = epInstallPath.substring(epInstallPath.lastIndexOf("/") + 1).substring(2);
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

	private boolean isUnix(StepContext context) {
		try {
			return context.get(Computer.class).isUnix();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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

class ConnectionAttempt extends BtcExecution {

	private static final long serialVersionUID = -8024389552732543068L;
	private BtcStartupStep step;

	public ConnectionAttempt(BtcStartupStep step, PrintStream logger, StepContext context) {
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
	
	@Override
	protected Object performAction() throws Exception {
		// don't add this step to the report...
		noReporting();
		
		if (step.getIpAddress() != null) {
			step.setPort(8080);
		}
		
		String host = step.getIpAddress() != null ? step.getIpAddress() : "localhost";
		
		log("Connecting to BTC EmbeddedPlatform (" + host + ":" + step.getPort() + "). This may take up to a minute...");
			
		checkArgument(host != null, "Cannot resolve agent IP address for remote connection.");
		ApiClient apiClient = new EPApiClient().setBasePath("http://" + host + ":" + step.getPort());
		apiClient.setReadTimeout(10000);
		Configuration.setDefaultApiClient(apiClient);
		HttpRequester.port = step.getPort();
		
		// Connect or startup EP
		boolean connected = HttpRequester.checkConnection("/ep/test", 200, logger());
		
		// this is a bit dirty... as the return object of this function is not available,
		// we use the data transfer object's status field instead, to indicate if the
		// connection could be established
		dataTransferObject.status = connected ? Status.OK : Status.ERROR;
		if (connected) {
			log("Successfully connected to BTC EmbeddedPlatform on " + host + ":" + step.getPort());
		}
		return connected;
	}
	
}

class WaitForConnection extends BtcExecution {

	private static final long serialVersionUID = -832370371669869980L;

	private BtcStartupStep step;
	private String epVersion = "";

	public WaitForConnection(BtcStartupStep step, PrintStream logger, StepContext context) {
		super(logger, context, step);
		this.step = step;
	}

	public Object performAction() throws Exception {
		// wait for ep rest service to respond
		boolean connected = HttpRequester.checkConnection("/ep/test", 200, step.getTimeout(), 2);
		if (connected) {
			log("Successfully connected to BTC EmbeddedPlatform " + epVersion + " on port " + step.getPort());
			response(200);
		} else {
			error("Connection timed out after " + step.getTimeout() + " seconds.");
			failed();
			return response(400);
		}
		return getResponse();
		
	}

}
