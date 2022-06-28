package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.remoting.RoleChecker;
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
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.ComputerPinger;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

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
		Store.matlabVersion = step.getMatlabVersion();
		if (!step.isSkipReportInitialization()) {
			initializeReporting();
		}
		
		// Prepare http connection
		List<String> hostNames = getContext().get(Computer.class).getChannel().call(new ListPossibleNames());
		String address = null;
		for (String hostName : hostNames) {
			InetAddress ia = InetAddress.getByName(hostName);
			if (ComputerPinger.checkIsReachable(ia, 3)) {
				address = hostName;
				break;
			}
		}
		checkArgument(address != null, "Cannot resolve agent IP address for remote connection.");
//		String hostName = getContext().get(Computer.class).getHostName();
		ApiClient apiClient = new EPApiClient().setBasePath("http://" + address + ":" + step.getPort());
//		ApiClient apiClient = new EPApiClient().setBasePath("http://localhost:" + step.getPort());
		apiClient.setReadTimeout(10000);
		Configuration.setDefaultApiClient(apiClient);
		HttpRequester.port = step.getPort();
		
		// Connect or startup EP
		boolean connected = HttpRequester.checkConnection("/ep/test", 200);
		if (connected) {
			response = 201;
		} else {
			// start command call can be skipped if we only connect to a starting instance
			if (!step.isSimplyConnect()) {
				// prepare ep start command
				List<String> command = createStartCommand();
				// start process and save it for future use (e.g. to destroy it)
				try {
					Store.epProcess = ProcessHelper.spawnManagedProcess(command, getContext());
				} catch (Exception e) {
					error("Failed to start BTC EmbeddedPlatform.", e);
					Store.epProcess.kill();
					return;
				}
			}

			// wait for ep rest service to respond
			connected = HttpRequester.checkConnection("/ep/test", 200, step.getTimeout(), 2, null);
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

	private boolean checkConnectionWithTimeout() throws IOException, InterruptedException, Exception {
		boolean connected = false;
		Callable<Boolean, Exception> exec = new Callable<Boolean, Exception>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Boolean call() throws Exception {
				return HttpRequester.checkConnection("/ep/test", 200, step.getTimeout(), 2, HttpRequester.printStream);
			}

			@Override
			public void checkRoles(RoleChecker checker) throws SecurityException {
			}
		};
		Launcher launcher = getContext().get(Launcher.class);
		if (launcher != null) {
			VirtualChannel channel = launcher.getChannel();
			if (channel == null) {
				throw new IllegalStateException("Launcher doesn't support remoting but it is required");
			}
			connected = channel.call(exec);
		}
		return connected;
	}
	private boolean checkConnectionOnce() throws IOException, InterruptedException, Exception {
		boolean connected = false;
		Callable<Boolean, Exception> exec = new Callable<Boolean, Exception>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Boolean call() throws Exception {
				return HttpRequester.checkConnection("/ep/test", 200, HttpRequester.printStream);
			}

			@Override
			public void checkRoles(RoleChecker checker) throws SecurityException {
			}
		};
		Launcher launcher = getContext().get(Launcher.class);
		if (launcher != null) {
			VirtualChannel channel = launcher.getChannel();
			if (channel == null) {
				throw new IllegalStateException("Launcher doesn't support remoting but it is required");
			}
			connected = channel.call(exec);
		}
		return connected;
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
		 * return the operating system of the Jenkins controller, not that of the agent
		 */
		if (isUnix()) {
			log("Starting BTC EmbeddedPlatform on Linux...");
			return createStartCommand_LINUX(licensingPackage);
		} else {
			// prepare data for process call
			String epInstallPath = getEpInstallPath().replaceAll("\\\\", "/");
			epVersion = epInstallPath.substring(epInstallPath.lastIndexOf("/") + 1).substring(2);
			log("Starting BTC EmbeddedPlatform on Windows...");
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
			FilePath epInstallPathResolved = resolveInAgentWorkspace(epInstallPath);
			FilePath jreParentDir = epInstallPathResolved.child("jres");
			checkArgument(jreParentDir != null, "Failed to find the Java directory in the EP installation ('" + jreParentDir + "')");
			Optional<FilePath> jdkBinDir = jreParentDir.listDirectories().stream().filter(dir -> dir.getName().startsWith("jdk")).findFirst();
			// may fail, then we get a warning
			String jreBinPath = jdkBinDir.get().getRemote() + "/bin";
			command.add("-vm");
			command.add("\"" + jreBinPath + "\"");
		} catch (Exception e) {
			warning("Could not determine proper jdk path for btc-embeddedplatform.");
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
		sb.append("\"\\${EP_INSTALL_PATH}/ep"); // path to ep is fixed based on docker image
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
		sb.append(" -Dep.rest.port=\\${REST_PORT}");
		if (step.getAdditionalJvmArgs() != null) {
			sb.append(" ").append(step.getAdditionalJvmArgs());
		}
		sb.append("\"");
		command.add(sb.toString());
		return command;
	}
	
	private static class ListPossibleNames extends MasterToSlaveCallable<List<String>,IOException> {
        /**
         * In the normal case we would use {@link Computer} as the logger's name, however to
         * do that we would have to send the {@link Computer} class over to the remote classloader
         * and then it would need to be loaded, which pulls in {@link Jenkins} and loads that
         * and then that fails to load as you are not supposed to do that. Another option
         * would be to export the logger over remoting, with increased complexity as a result.
         * Instead we just use a logger based on this class name and prevent any references to
         * other classes from being transferred over remoting.
         */
        private static final Logger LOGGER = Logger.getLogger(ListPossibleNames.class.getName());

        public List<String> call() throws IOException {
            List<String> names = new ArrayList<>();

            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni =  nis.nextElement();
                LOGGER.log(Level.FINE, "Listing up IP addresses for {0}", ni.getDisplayName());
                Enumeration<InetAddress> e = ni.getInetAddresses();
                while (e.hasMoreElements()) {
                    InetAddress ia =  e.nextElement();
                    if(ia.isLoopbackAddress()) {
                        LOGGER.log(Level.FINE, "{0} is a loopback address", ia);
                        continue;
                    }

                    if(!(ia instanceof Inet4Address)) {
                        LOGGER.log(Level.FINE, "{0} is not an IPv4 address", ia);
                        continue;
                    }

                    LOGGER.log(Level.FINE, "{0} is a viable candidate", ia);
                    names.add(ia.getHostAddress());
                }
            }
            return names;
        }
        private static final long serialVersionUID = 1L;
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

	/*
	 * End of getter/setter section
	 */

} // end of step class
