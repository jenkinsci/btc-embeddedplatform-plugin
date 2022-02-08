package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FilenameFilter;
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
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcStartupStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private BtcStartupStep step;

    public BtcStartupStepExecution(BtcStartupStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected void performAction() throws Exception {
        // don't add this step to the report
        noReporting();

        // Prepare http connection
        ApiClient apiClient =
            new EPApiClient().setBasePath("http://localhost:" + step.getPort());
        Configuration.setDefaultApiClient(apiClient);
        HttpRequester.port = step.getPort();
        Store.startDate = new Date();
        boolean connected = HttpRequester.checkConnection("/test", 200);
        String epVersion = "";
        if (connected) {
            response = 201;
        } else {
            // start command call can be skipped if we only connect to a starting instance (e.g. in docker)
            if (!step.isSimplyConnect()) {
            	// prepare data for process call
            	epVersion = new File(step.getInstallPath()).getName().trim().substring(2); // D:/Tools/BTC/ep2.9p0 -> 2.9p0
            	String jreDirectory = getJreDir();
            	String licensingPackage = step.getLicensingPackage();
            	
            	// Check preconditions
                checkArgument(step.getInstallPath() != null && new File(step.getInstallPath()).exists(),
                    "Provided installPath '" + step.getInstallPath() + "' cannot be resolved.");
                File epExecutable = new File(step.getInstallPath() + "/rcp/ep.exe");
                checkArgument(epExecutable.exists(),
                    "BTC EmbeddedPlatform executable cannot be found in " + epExecutable.getCanonicalPath());

                // prepare ep start command
                List<String> command =
                    createStartCommand(epExecutable, epVersion, jreDirectory, licensingPackage, step.getPort());
                ProcessBuilder pb = new ProcessBuilder(command);
                // start process and save it for future use (e.g. to destroy it)
                Store.epProcess = pb.start();
                log(String.join(" ", command));
            }

            // wait for ep rest service to respond
            connected = HttpRequester.checkConnection("/test", 200, step.getTimeout(), 2);
            if (connected) {
                log("Successfully connected to BTC EmbeddedPlatform " + epVersion
                    + " on port " + step.getPort());
                response = 200;
            } else {
                log("Connection timed out after " + step.getTimeout() + " seconds.");
                failed();
                // Kill EmbeddedPlatform process to prevent zombies!
                Store.epProcess.destroyForcibly();
                response = 400;
                return;
            }
        }
        initializeReporting();
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
     * @param epExecutable ep.exe (File)
     * @param epVersion ep version string
     * @param jreDirectory jreDirectory
     * @param licensingPackage license package
     * @param port the port to use
     * @return the ep start command
     * @throws IOException path issues...
     */
    private List<String> createStartCommand(File epExecutable, String epVersion, String jreDirectory,
        String licensingPackage, int port) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("\"" + epExecutable.getCanonicalPath() + "\"");
        command.add("-clearPersistedState");
        command.add("-application");
        command.add("ep.application.headless");
        command.add("-nosplash");
        command.add("-vm");
        command.add("\"" + jreDirectory + "\"");
        command.add("-vmargs");
        command.add("-Dep.runtime.batch=ep");
        command.add("-Dosgi.configuration.area.default=@user.home/AppData/Roaming/BTC/ep/" +
            epVersion + "/" + port + "/configuration");
        command.add("-Dosgi.instance.area.default=@user.home/AppData/Roaming/BTC/ep/" + epVersion + "/"
            + port + "/workspace");
        command.add("-Dep.configuration.logpath=AppData/Roaming/BTC/ep/" + epVersion + "/"
            + port + "/logs");
        command.add("-Dep.runtime.workdir=BTC/ep/" + epVersion + "/" + port);
        command.add("-Dep.licensing.package=" + licensingPackage);
        command.add("-Dep.rest.port=" + port);
        command.add("-Djna.nosys=true");
        command.add("-Dprism.order=sw");
        command.add("-XX:+UseParallelGC");
        if (step.getAdditionalJvmArgs() != null) {
            command.add(step.getAdditionalJvmArgs());
        }
        return command;
    }

    private String getJreDir() {
        File jreParentDir = new File(step.getInstallPath() + "/jres");
        File[] javaDirs = jreParentDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("jdk");

            }
        });
        checkArgument(javaDirs.length > 0, "Failed to find the Java runtime in " + jreParentDir.getPath());
        String jreBinPath = javaDirs[0].getPath() + "/bin";
        return jreBinPath;
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
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
         * This specifies the step name that the the user can use in his Jenkins Pipeline
         * - for example: btcStartup installPath: 'C:/Program Files/BTC/ep2.9p0', port: 29267
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
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
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

    /*
     * End of getter/setter section
     */

} // end of step class
