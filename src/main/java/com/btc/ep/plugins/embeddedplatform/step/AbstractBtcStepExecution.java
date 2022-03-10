package com.btc.ep.plugins.embeddedplatform.step;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
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
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Store;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import htmlpublisher.HtmlPublisher;
import htmlpublisher.HtmlPublisherTarget;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.model.Run;
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

	public AbstractBtcStepExecution(Step step, StepContext context) {
		super(context);
		this.functionName = step.getDescriptor().getFunctionName();
		this.reportingStep = new TestStep(functionName.replace("btc", "")); // TODO: make this more user friendly
		recordStepArguments(step);
	}

	private boolean reportingDisabled = false;
	public TimerTask t;

	public boolean start() {
		TimerTask t = new TimerTask() {

			@Override
			public void run() {
				Date t1 = new Date();
				try {
					jenkinsConsole = getContext().get(TaskListener.class).getLogger();
					// set default print stream to jenkinsConsole, relevant for waitForCompletion
					// method
					HttpRequester.printStream = jenkinsConsole;
					/*
					 * Main action (implemented by the individual steps)
					 */
					performAction();

					getContext().onSuccess(response); // return to context
				} catch (Exception e) {
					if (e instanceof ApiException && jenkinsConsole != null) {
						String responseBody = ((ApiException) e).getResponseBody();
						String msg = "Error during call of " + functionName + "(): " + responseBody;
						log(msg);
						info(msg);
					} else {
						info("Error: " + e.getMessage());
						e.printStackTrace(jenkinsConsole); // print stack trace to Jenkins Console
					}
					error();
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
	 * @param data     the data object containing the step arguments
	 * @param testStep the test step for reporting (hint: A {@link BasicStep} should
	 *                 usually also be a {@link TestStep} and can be cast
	 *                 accordingly)
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
					((TestStep) reportingStep).addArgument(argName, value.toString());
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
	 * Updates the step's status to the given value. The status is initially OK and
	 * can only be made worse. If the status is ERROR, a call of status(Status.OK)
	 * will have no effect
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
	 * Reporting Option: Adds the specified text to the details and the
	 * corresponding link to the current step
	 *
	 * @param detail the detail text to add
	 * @param link   the detail's link to add
	 */
	public AbstractBtcStepExecution detailWithLink(String detail, String link) {
		((TestStep) this.reportingStep).addDetail(detail);
		((TestStep) this.reportingStep).addDetailsLink(link);
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
	 * Sets the result to SKIPPED. Also sets StatusOK and StatusWarning to false
	 * (required for correct reporting)
	 */
	public AbstractBtcStepExecution skipped() {
		this.reportingStep.setStatusOK(false);
		this.reportingStep.setStatusWARNING(false);
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
		// also set the build result to failure
		try {
			getContext().get(Run.class).setResult(Result.FAILURE);
		} catch (Exception e) {
		}
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
	 * Writes the given message to the jenkins console output. All messages are
	 * prefixed with "[BTC] "
	 * 
	 * @param message
	 */
	protected void log(String message) {
		jenkinsConsole.print("[BTC] ");
		jenkinsConsole.println(message);
	}

	/**
	 * Writes the given message to the jenkins console output. The message is
	 * formatted with the given args: String.format(message, args) All messages are
	 * prefixed with "[BTC] "
	 * 
	 * @param message
	 */
	protected void log(String message, Object... formatArgs) {
		log(String.format(message, formatArgs));
	}

	/**
	 * Launches a process with the given command using hudson.Launcher.class, starts
	 * it and returns the process. The process inherits the environment from Jenkins
	 * which allows Jenkins to kill processes when the run finishes/aborts/exits
	 * prematurely, etc.
	 * 
	 * @param command the command to run
	 * @return the process object
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected Proc spawnManagedProcess(List<String> command) throws IOException, InterruptedException {
		// Use magical "get(...)" command from context to get the Launcher
		Launcher launcher = getContext().get(Launcher.class);

		// Use magical "get(...)" command from context to get the set of EnvVars that
		// contains the required cookie
		// This cookie is some sort of hash that jenkins needs to be able to kill a
		// spawned process
		Map<String, String> taskHandle = new HashMap<>();
		String cookie = getContext().get(hudson.EnvVars.class).get(JENKINS_NODE_COOKIE);
		taskHandle.put(JENKINS_NODE_COOKIE, cookie);

		// Start external process with the identifying cookie and return the process
		// object
		Proc process = launcher.launch().cmds(command).envs(taskHandle).quiet(true).start();
		return process;
	}

	/**
	 * Converts an absolute or relative path into a Path object.
	 *
	 * @param filePathString An absolute or relative path (relative to the pipelines
	 *                       pwd)
	 * @return the path object
	 * @throws Exception
	 */
	protected String toRemoteAbsolutePathString(String filePathString) throws Exception {
		if (filePathString != null) {
			try {
				if (isPathAbsolute(filePathString)) {
					return filePathString;
				} else {
					FilePath workspace = getContext().get(FilePath.class);
					return workspace.getRemote() + "/" + filePathString;
				}
			} catch (Exception e) {
				log("Cannot resolve path: " + filePathString);
				error();
			}
		}
		return null;
	}

	protected FilePath resolveInAgentWorkspace(String relOrAbsPathInWorkspace) throws Exception {
		return getContext().get(FilePath.class).child(relOrAbsPathInWorkspace);
	}

	/**
	 * Returns true if the string counts as an absolute path on the executor (os
	 * dependend)
	 * 
	 * @param filePathString must not be null
	 * @return true if absolute path
	 * @throws Exception
	 */
	protected boolean isPathAbsolute(String filePathString) throws Exception {
		return (isUnix() && filePathString.startsWith("/")) || (!isUnix() && filePathString.contains(":"));
	}

	protected Boolean isUnix() throws Exception {
		return getContext().get(Computer.class).isUnix();
	}

	/**
	 * Returns a resolved profilePath that uses the give profilePath (may be
	 * relative) if not null. Otherwise it derives the path from the model path or,
	 * if that is null, it returns a fallback "profile.epp" in the workspace root.
	 * 
	 * @param stepProfilePath the profile path, may be relative, may be null
	 * @param modelPath       the resolved model path, may be null
	 * @return a resolved profile path
	 * @throws Exception
	 */
	protected String getProfilePathOrDefault(String stepProfilePath) throws Exception {
		if (stepProfilePath != null) {
			// resolve selected string
			return toRemoteAbsolutePathString(stepProfilePath);
		} else {
			return toRemoteAbsolutePathString("profile.epp");
		}
	}

	/**
	 * Uses the HTML Publisher Plugin to publish an HTML file in Jenkins. This makes
	 * it available on the Job page for easy access.
	 * 
	 * @param reportTitle the report's title
	 * @param fileName    name of the file to publish
	 */
	protected void publishHtml(String reportTitle, String fileName) throws Exception {
		String reportDir = Store.exportPath.replace("\\", "/");
		HtmlPublisherTarget target = new HtmlPublisherTarget(reportTitle, reportDir, fileName, true, // <-- store all
																										// for builds,
																										// not just
																										// latest
				true, // <-- link should reference last build, not last successful
				true); // <-- build should not fail if the html file is not present
		HtmlPublisher.publishReports(getContext().get(Run.class), getContext().get(FilePath.class),
				getContext().get(TaskListener.class), Collections.singletonList(target), HtmlPublisher.class);
	}

	/**
	 * Implemented by the individual btc step executors.
	 * 
	 * @throws Exception
	 */
	protected abstract void performAction() throws Exception;
}
