package com.btc.ep.plugins.embeddedplatform.step.basic;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

import com.btc.ep.plugins.embeddedplatform.http.ApiClientThatDoesntSuck;
import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

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
    private String additionalJvmArgs;
    private int timeout = 120;
    private int port = 29267;

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

    /**
     * Get installPath.
     * 
     * @return the installPath
     */
    public String getInstallPath() {
        return installPath;
    }

    /**
     * Set installPath.
     * 
     * @param installPath the installPath to set
     */
    @DataBoundSetter
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    /**
     * Get port.
     * 
     * @return the port
     */
    public int getPort() {
        return port;

    }

    /**
     * Set port.
     * 
     * @param port the port to set
     */
    @DataBoundSetter
    public void setPort(int port) {
        this.port = port;

    }

    /**
     * Get licensingPackage.
     * 
     * @return the licensingPackage
     */
    public String getLicensingPackage() {
        return licensingPackage;

    }

    /**
     * Set licensingPackage.
     * 
     * @param licensingPackage the licensingPackage to set
     */
    @DataBoundSetter
    public void setLicensingPackage(String licensingPackage) {
        this.licensingPackage = licensingPackage;

    }

    /**
     * Get additionalJvmArgs.
     * 
     * @return the additionalJvmArgs
     */
    public String getAdditionalJvmArgs() {
        return additionalJvmArgs;

    }

    /**
     * Set additionalJvmArgs.
     * 
     * @param additionalJvmArgs the additionalJvmArgs to set
     */
    @DataBoundSetter
    public void setAdditionalJvmArgs(String additionalJvmArgs) {
        this.additionalJvmArgs = additionalJvmArgs;

    }

    /**
     * Get timeout.
     * 
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;

    }

    /**
     * Set timeout.
     * 
     * @param timeout the timeout to set
     */
    @DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;

    }

    /*
     * End of getter/setter section
     */

} // end of step class

/**
 * This class defines what happens when the above step is executed
 */
class BtcStartupStepExecution extends StepExecution {

    private static final long serialVersionUID = 1L;

    private BtcStartupStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private String status;

    /**
     * Constructor
     *
     * @param btcStartupStep
     * @param context
     */
    public BtcStartupStepExecution(BtcStartupStep btcStartupStep, StepContext context) {
        super(context);
        this.step = btcStartupStep;
    }

    /*
     * The start method must either
     * - start an asychronous process in its own thread (e.g. TimerTask) and then return false or
     * - perform the desired action immediately (shouldn't take more that 1-2 seconds) and then return true
     */
    @Override
    public boolean start() throws Exception {
        /*
         * We can use something like this timer task to implement the desired action, i.e.:
         * - process the step parameters
         * - check preconditions
         * -> is the API already connected?
         * -> is EP in the expected state (e.g. profile loaded, architecture imported, test cases available...)?
         * -> do all referenced files exist?
         * - perform action using EP SDK
         */
        TimerTask t = new TimerTask() {

            @Override
            public void run() {
                // This is what's actually executed (currently just prints some text to the Jenkins console):
                try {
                    PrintStream jenkinsConsole = getContext().get(TaskListener.class).getLogger();
                    // Prepare http connection
                    ApiClient apiClient =
                        new ApiClientThatDoesntSuck().setBasePath("http://localhost:" + step.getPort());
                    Configuration.setDefaultApiClient(apiClient);
                    HttpRequester.port = step.getPort();

                    boolean connected = HttpRequester.checkConnection("/test", 200);
                    if (connected) {
                        jenkinsConsole
                            .println("Successfully connected to a running instance of BTC EmbeddedPlatform on port "
                                + step.getPort());
                    } else {
                        // Check preconditions
                        checkArgument(step.getInstallPath() != null && new File(step.getInstallPath()).exists(),
                            "Provided installPath '" + step.getInstallPath() + "' cannot be resolved.");
                        File epExecutable = new File(step.getInstallPath() + "/rcp/ep.exe");
                        checkArgument(epExecutable.exists(),
                            "BTC EmbeddedPlatform executable cannot be found in " + epExecutable.getCanonicalPath());

                        // prepare data for process call
                        String epVersion = new File(step.getInstallPath()).getName().trim().substring(2); // D:/Tools/BTC/ep2.9p0 -> 2.9p0
                        String jreDirectory = getJreDir();
                        String licensingPackage = step.getLicensingPackage();

                        // prepare ep start command
                        List<String> command =
                            createStartCommand(epExecutable, epVersion, jreDirectory, licensingPackage, step.getPort());
                        ProcessBuilder pb = new ProcessBuilder(command);
                        // start process and save it for future use (e.g. to destroy it)
                        Store.epProcess = pb.start();
                        jenkinsConsole.println(String.join(" ", command));

                        // wait for ep rest service to respond
                        connected = HttpRequester.checkConnection("/test", 200, step.getTimeout(), 2);
                        if (connected) {
                            jenkinsConsole.println("Successfully connected to BTC EmbeddedPlatform " + epVersion
                                + " on port " + step.getPort());
                        } else {
                            jenkinsConsole.println("Connection timed out after " + step.getTimeout() + " seconds.");
                        }
                    }
                    getContext().onSuccess("Done");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    getContext().onFailure(e1);
                }
            }

        };
        // trigger the desired action by calling the run() method
        t.run();
        // return false (see explanation in comment on start() method)
        return false;
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
        command.add("com.btc.ep.application.headless");
        command.add("-nosplash");
        command.add("-vm");
        command.add("\"" + jreDirectory + "\"");
        command.add("-vmargs");
        command.add("-Dep.runtime.batch=com.btc.ep");
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

    @Override
    public String getStatus() {
        return status;
    }

}
