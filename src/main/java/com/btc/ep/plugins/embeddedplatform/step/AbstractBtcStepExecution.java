package com.btc.ep.plugins.embeddedplatform.step;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.openapitools.client.ApiException;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.reporting.project.BasicStep;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStep;
import com.btc.ep.plugins.embeddedplatform.test.TestStuff_FakeProc;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;

public abstract class AbstractBtcStepExecution extends StepExecution {

	private static final String JENKINS_NODE_COOKIE = "JENKINS_NODE_COOKIE";
    private static final long serialVersionUID = 1L;
    protected Status status = Status.OK;
    protected PrintStream jenkinsConsole;
    /**
     * Response returned to Jenkins (may be overwritten by implementation)
     */
    protected Object response = 200;
    private String functionName;
    private BasicStep reportingStep;

    /*
     * --------------- ORIGINAL: START ---------------
     */
    /*public AbstractBtcStepExecution(Step step, StepContext context) {
        super(context);
        this.functionName = step.getDescriptor().getFunctionName();
        this.reportingStep = new TestStep(functionName);
        recordStepArguments(step);
    }*/

    /*
     * -------------- ORIGINAL: END -----------------
     */

    /*
     * --------------- Testing: START -----------------
     */
	@SuppressWarnings("deprecation")
	public AbstractBtcStepExecution(Step step, StepContext context) {
    	this.context = context;
    	this.functionName = "DUMMY";
    	this.reportingStep = new TestStep(functionName);
    	recordStepArguments(step);
    }
    private StepContext context;
    @Override
    public StepContext getContext() {
    	return this.context;
    }
    /*
     * --------------- Testing: END -----------------
     */
    
    
    private boolean reportingDisabled = false;


    public boolean start() throws Exception {
        TimerTask t = new TimerTask() {

            @Override
            public void run() {
                Date t1 = new Date();
                try {
                    jenkinsConsole = getContext().get(TaskListener.class).getLogger();
                    // set default print stream to jenkinsConsole, relevant for waitForCompletion method
                    HttpRequester.printStream = jenkinsConsole;
                    /*
                     * Main action (implemented by the individual steps)
                     */
                    performAction();

                    getContext().onSuccess(response); // return to context
                } catch (Exception e) {
                    if (e instanceof ApiException && jenkinsConsole != null) {
                        String responseBody = ((ApiException)e).getResponseBody();
                        log(
                            "Error during call of " + functionName + "(): " + responseBody);
                    }
                    status(Status.ERROR);
                    getContext().onFailure(e); // return to context
                } finally {
                    if (!reportingDisabled) {
                        addStepToReportData(t1);
                    }
                }

            }

        };
        t.run();
        return false;
    }

    /**
     * Invokes all getters (0-parameter methods that start with "get" or "is") of
     * the data object and adds the respective values to the test step's argument
     * list.
     *
     * @param data
     *            the data object containing the step arguments
     * @param testStep
     *            the test step for reporting (hint: A {@link BasicStep} should
     *            usually also be a {@link TestStep} and can be cast accordingly)
     */
    private void recordStepArguments(Step step) {
        for (Method method : step.getClass().getDeclaredMethods()) {
            String methodName = method.getName();
            if (method.getParameterCount() == 0) {
                String argName;
                Object value = null;
                if (methodName.startsWith("get")) {
                    argName = methodName.substring(3, methodName.length());
                } else if (methodName.startsWith("is")) {
                    argName = methodName.substring(2, methodName.length());
                } else {
                    continue;
                }
                try {
                    value = method.invoke(step);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (value != null) {
                    ((TestStep)reportingStep).addArgument(argName, value.toString());
                }
            }
        }
    }

    /**
     * Adds the step to the stored testStepSection
     * 
     * @param t1 the starting time used to calculate the total runtime
     */
    private void addStepToReportData(Date t1) {
        checkArgument(reportingStep.getDetails().size() == reportingStep.getDetailsLinks().size(),
            "Reporting: All details must have a corresponding link.");

        reportingStep.setStatusOK(status == Status.OK);
        reportingStep.setStatusWARNING(status == Status.WARNING);
        reportingStep.setTime(Util.getTimeDiffAsString(t1, new Date()));
        Store.testStepSection.addStep(reportingStep);
    }

    /**
     * Updates the step's status to the given value. The status is initially OK and can only be made worse.
     * If the status is ERROR, a call of status(Status.OK) will have no effect
     * 
     * @param status
     */
    public AbstractBtcStepExecution status(Status status) {
        // only modify status to worse states
        if (this.status.compareTo(status) < 0) {
            this.status = status;
        }
        return this;
    }

    /**
     * Reporting Option: Sets the info text for the current step
     *
     * @param info the text to set
     */
    public AbstractBtcStepExecution info(String info) {
        this.reportingStep.setInfo(info);
        return this;
    }

    /**
     * Reporting Option: Adds the specified text to the details and the corresponding link to the current step
     *
     * @param detail the detail text to add
     * @param link the detail's link to add
     */
    public AbstractBtcStepExecution detailWithLink(String detail, String link) {
        ((TestStep)this.reportingStep).addDetail(detail);
        ((TestStep)this.reportingStep).addDetailsLink(link);
        return this;
    }

    /**
     * Prevents the step from being added to the report
     *
     */
    public AbstractBtcStepExecution noReporting() {
        this.reportingDisabled = true;
        return this;
    }

    /**
     * Sets the result to PASSED
     */
    public AbstractBtcStepExecution passed() {
        this.reportingStep.setPassed(true);
        return this;
    }

    /**
     * Sets the result to SKIPPED
     */
    public AbstractBtcStepExecution skipped() {
        this.reportingStep.setSkipped(true);
        return this;
    }

    /**
     * Sets the result text (PASSED, FAILED, ERROR, etc.)
     */
    public AbstractBtcStepExecution result(String resultText) {
        this.reportingStep.setResult(resultText);
        return this;
    }

    /**
     * Sets the result to FAILED
     */
    public AbstractBtcStepExecution failed() {
        this.reportingStep.setPassed(false);
        return this;
    }
    
    /**
     * Sets the status to ERROR
     */
    public AbstractBtcStepExecution error() {
        this.reportingStep.setStatusOK(false);
        status(Status.ERROR);
        return this;
    }
    
    /**
     * Sets the status to warning
     */
    public AbstractBtcStepExecution warning() {
        this.reportingStep.setStatusWARNING(true);
        status(Status.WARNING);
        return this;
    }

    @Override
    public String getStatus() {
        return status.toString();
    }

    /**
     * Writes the given message to the jenkins console output.
     * All messages are prefixed with "[BTC] "
     * @param message
     */
    protected void log(String message) {
    	jenkinsConsole.print("[BTC] ");
    	jenkinsConsole.println(message);
    }
    
    /**
     * Launches a process with the given command using hudson.Launcher.class, starts it and returns the process.
     * The process inherits the environment from Jenkins which allows Jenkins to kill processes when the run finishes/aborts/exits prematurely, etc.
     * 
     * @param command the command to run
     * @return the process object
     * @throws IOException
     * @throws InterruptedException
     */
    protected Proc spawnManagedProcess(List<String> command) throws IOException, InterruptedException {
    	// Shortcut for testing outside of the Jenkins context 
    	if ("DUMMY".equals(this.functionName)) {
    		return new TestStuff_FakeProc(command);
    	}
    	
    	// Use magical "get(...)" command from context to get the Launcher
    	Launcher launcher = getContext().get(Launcher.class);
    	
    	// Use magical "get(...)" command from context to get the set of EnvVars that contains the required cookie
    	// This cookie is some sort of hash that jenkins needs to be able to kill a spawned process
    	Map<String, String> taskHandle = new HashMap<>();
    	String cookie = getContext().get(hudson.EnvVars.class).get(JENKINS_NODE_COOKIE);
    	taskHandle.put(JENKINS_NODE_COOKIE, cookie);
    	
    	// Start external process with the identifying cookie and return the process object
    	Proc process = launcher.launch().cmds(command).envs(taskHandle).start();
		return process;
	}
    
    /**
     * Converts an absolute or relative path into a Path object.
     *
     * @param filePathString An absolute or relative path (relative to the pipelines pwd)
     * @return the path object
     * @throws Exception 
     */
    protected Path resolvePath(String filePathString) throws Exception {
        if (filePathString != null) {
            try {
                Path path = Paths.get(filePathString);
                if (path.isAbsolute()) {
                    return path;
                } else {
                    FilePath workspace = getContext().get(FilePath.class);
                    String baseDir = Paths.get(workspace.toURI()).toString();
                    path = new File(baseDir, path.toString()).toPath();
                    return path;
                }
            } catch (Exception e) {
                log("Cannot resolve path: " + filePathString);
                error();
            }
        }
        return null;
    }
    
    /**
     * Returns a resolved profilePath that uses the give profilePath (may be relative)
     * if not null. Otherwise it derives the path from the model path or, if that is null,
     * it returns a fallback "profile.epp" in the workspace root.
     * 
     * @param stepProfilePath the profile path, may be relative, may be null
     * @param modelPath the resolved model path, may be null
     * @return a resolved profile path
     * @throws Exception 
     */
    protected Path getProfilePathOrDefault(String stepProfilePath, Path modelPath) throws Exception {
		if (stepProfilePath != null) {
			// resolve selected string to a path
			return resolvePath(stepProfilePath);
		} else {
			// no profilePath specified, use model name or fallback value
			if (modelPath == null) {
				// fallback
				return resolvePath("profile.epp");
			} else {
				// derive epp name from model file name
				String modelFileName = modelPath.toFile().getName();
				if (modelFileName.contains(".")) {
					modelFileName = modelFileName.substring(0, modelFileName.lastIndexOf("."));
				}
				String eppName = modelFileName + ".epp";
				return resolvePath(eppName);
			}
		}
	}

    /**
     * Implemented by the individual btc step executors.
     * 
     * @throws Exception
     */
    protected abstract void performAction() throws Exception;
}
