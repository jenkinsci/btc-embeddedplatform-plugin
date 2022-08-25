package com.btc.ep.plugins.embeddedplatform.step;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.MatlabScriptExecutionApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.MatlabScriptInput;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.project.BasicStep;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStep;
import com.btc.ep.plugins.embeddedplatform.util.Status;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.CloseProofOutputStream;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.remoting.RemoteOutputStream;
import jenkins.security.MasterToSlaveCallable;

public abstract class BtcExecution extends MasterToSlaveCallable<DataTransferObject, Exception> {

	private static final long serialVersionUID = 1L;
	private final OutputStream remoteLogger;
	private transient PrintStream localLogger;

	private String functionName;

	public static final String TRUE  = "TRUE";
	public static final String FALSE = "FALSE";
	
	public DataTransferObject dataTransferObject;
	protected Result buildResult = Result.NOT_BUILT;
	protected boolean isUnix;
	protected String workspace;
	
	protected abstract Object performAction() throws Exception;
	
	public BtcExecution(PrintStream logger, StepContext context, Step step) {
		this.localLogger = logger;
		this.remoteLogger = new RemoteOutputStream(new CloseProofOutputStream(logger));
		this.functionName = step.getDescriptor().getFunctionName();
		this.dataTransferObject = new DataTransferObject();
		this.dataTransferObject.reportingStep = new TestStep(functionName.replace("btc", "")); // TODO: make this more user friendly
		try {
			this.workspace = context.get(FilePath.class).getRemote();
		} catch (Exception e) {
			e.printStackTrace();
		}
		isUnix(context);
		recordStepArguments(step);
	}

	@Override
	public DataTransferObject call() throws Exception {
		try {
			// invoke method defined by child class
			Object result = performAction();
			logger().println("Step function result: " + result);
			
			buildResult = Result.SUCCESS;
		} catch (Exception e) {
			error("Error during call of " + functionName + "():", e);
		}
		return dataTransferObject;
	}
	
	/**
	 * Resolves the given String to an absolute Path String
	 * @param absOrRelPath the path string
	 * @return the absolute path string (may be null)
	 */
	public String resolveToString(String absOrRelPath) {
		if (absOrRelPath != null) {
			Path p = Paths.get(absOrRelPath).isAbsolute() ? Paths.get(absOrRelPath) : new File(workspace + File.separator + absOrRelPath).getAbsoluteFile().toPath();
			return p.toString();
		}
		return null;
	}
	
	/**
	 * Resolves the given String to a Path object
	 * @param absOrRelPath the path string
	 * @return the path object (may be null)
	 */
	public Path resolveToPath(String absOrRelPath) {
		Path p = Paths.get(absOrRelPath).isAbsolute() ? Paths.get(absOrRelPath) : new File(workspace + File.separator + absOrRelPath).toPath();
		return p;
	}
	
	/**
	 * Updates the step's status to the given value. The status is initially OK and
	 * can only be made worse. If the status is ERROR, a call of status(Status.OK)
	 * will have no effect
	 * 
	 * @param status
	 */
	public BtcExecution status(Status status) {
		// only modify status to worse states
		if (this.dataTransferObject.status.compareTo(status) < 0) {
			this.dataTransferObject.status = status;
		}
		return this;
	}

	/**
	 * Reporting Option: Sets the info text for the current step
	 *
	 * @param info the text to set
	 */
	public BtcExecution info(String info) {
		this.dataTransferObject.reportingStep.setInfo(info);
		return this;
	}

	/**
	 * Reporting Option: Adds the specified text to the details and the
	 * corresponding link to the current step
	 *
	 * @param detail the detail text to add
	 * @param link   the detail's link to add
	 */
	public BtcExecution detailWithLink(String detail, String link) {
		((TestStep) this.dataTransferObject.reportingStep).addDetail(detail);
		((TestStep) this.dataTransferObject.reportingStep).addDetailsLink(link);
		return this;
	}

	/**
	 * Prevents the step from being added to the report
	 *
	 */
	public BtcExecution noReporting() {
		this.dataTransferObject.reportingDisabled  = true;
		return this;
	}

	/**
	 * Sets the result to PASSED
	 */
	public BtcExecution passed() {
		this.dataTransferObject.reportingStep.setPassed(true);
		this.dataTransferObject.reportingStep.setFailed(false);
		return this;
	}

	/**
	 * Sets the result to SKIPPED. Also sets StatusOK and StatusWarning to false
	 * (required for correct reporting)
	 */
	public BtcExecution skipped() {
		this.dataTransferObject.reportingStep.setStatusOK(false);
		this.dataTransferObject.reportingStep.setStatusWARNING(false);
		this.dataTransferObject.reportingStep.setSkipped(true);
		return this;
	}

	/**
	 * Sets the result text (PASSED, FAILED, ERROR, etc.)
	 */
	public BtcExecution result(String resultText) {
		this.dataTransferObject.reportingStep.setResult(resultText);
		return this;
	}

	/**
	 * Sets the result to FAILED
	 */
	public BtcExecution failed() {
		this.dataTransferObject.reportingStep.setPassed(false);
		this.dataTransferObject.reportingStep.setFailed(true);
		return this;
	}

	/**
	 * Sets the status to ERROR
	 */
	public BtcExecution error() {
		this.dataTransferObject.reportingStep.setStatusOK(false);
		status(Status.ERROR);
		// also set the build result to failu)re
		buildResult = Result.FAILURE;
		return this;
	}

	/**
	 * Sets the status to ERROR and logs an error message to the jenkinsConsole
	 */
	public BtcExecution error(String message) {
		return error(message, null);
	}

	/**
	 * Sets the status to ERROR and logs an error message and exception to the
	 * jenkinsConsole
	 */
	public BtcExecution error(String message, Throwable t) {
		String reportingInfo = message;
		if (t != null) {
			// print response if available
			if (t instanceof ApiException) {
				String responseBody = ((ApiException) t).getResponseBody();
				reportingInfo += String.format(" (%s)", responseBody);
			}
		}
		log("Error: " + reportingInfo);
		info(reportingInfo);
		if (t != null) {
			// print full stack trace
			try {
				t.printStackTrace(logger());
			} catch (Exception e) {
				t.printStackTrace();
			}
		}
		return error();
	}

	/**
	 * Sets the status to warning and logs the given message.
	 */
	public BtcExecution warning(String message) {
		return warning(message, null);
	}

	/**
	 * Sets the status to warning, logs the given message and prints the stack
	 * trace.
	 */
	public BtcExecution warning(String message, Throwable t) {
		log("Warning: " + message);
		if (t != null) {
			// print response if available
			if (t instanceof ApiException) {
				log(((ApiException) t).getResponseBody());
			}
			// print full stack trace
			try {
				t.printStackTrace(logger());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return warning();
	}

	/**
	 * Sets the status to warning
	 */
	public BtcExecution warning() {
		this.dataTransferObject.reportingStep.setStatusWARNING(true);
		status(Status.WARNING);
		return this;
	}

	public String getStatus() {
		return this.dataTransferObject.status.toString();
	}

	/**
	 * Writes the given message to the jenkins console output. All messages are
	 * prefixed with "[BTC] "
	 * 
	 * @param message
	 */
	protected void log(String message) {
		logger().print("[BTC] ");
		logger().println(message);
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
	 * Returns the file or directory name.
	 * 
	 * @param path            must not be null
	 * @param removeExtension only returns the name, removing anything after the
	 *                        last . (incl. the dot)
	 * @return the file or directory name
	 */
	protected String getFileName(String path, boolean removeExtension) {
		String[] parts = path.replace("\\", "/").split("/");
		String name = parts[parts.length - 1];
		if (removeExtension && name.contains(".")) {
			name = name.substring(0, name.lastIndexOf(".") - 1);
		}
		return name;
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
		return (isUnix && filePathString.startsWith("/")) || (!isUnix && filePathString.contains(":"));
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
			return Paths.get(stepProfilePath).toAbsolutePath().toString();
		} else {
			return Paths.get(workspace + "/profile.epp").toAbsolutePath().toString();
		}
	}

	/**
	 * Configures the ML connection and runs the startup script (if specified)
	 * 
	 * @throws Exception
	 */
	protected void prepareMatlab(MatlabAwareStep step) throws Exception {
		String matlabVersionOrEmptyString = step.getMatlabVersion();
		if (matlabVersionOrEmptyString == null) {
			matlabVersionOrEmptyString = dataTransferObject.matlabVersion == null ? "" : dataTransferObject.matlabVersion;
		}
		// this is only needed if we have a startup script or the user specified a Matlab version
		if (dataTransferObject.matlabVersion != null || step.getMatlabVersion() != null || step.getStartupScriptPath() != null) {
			log("Preparing Matlab " + matlabVersionOrEmptyString + "...");
			Util.configureMatlabConnection(matlabVersionOrEmptyString, step.getMatlabInstancePolicy());
			runMatlabStartupScript(step);
//			log("Successfully prepared Matlab " + matlabVersionOrEmptyString);
		}
		
	}

	/*
	 * Runs the matlab startup script.
	 * 
	 * Input matches this pattern: <scriptPath> <arg0> <arg1> ... <argN>. Script
	 * path can be absolute or relative to the workspace
	 * 
	 * "myscript.m" - "folder/myscript.m" - "E:/folder/myscript.m" -
	 * "/home/thabok/myscript.m"
	 * 
	 * and possibly with args
	 * 
	 * - "myscript.m arg0 arg1 arg2"
	 */
	private void runMatlabStartupScript(MatlabAwareStep step) throws Exception, ApiException {
		MatlabScriptExecutionApi mlApi = new MatlabScriptExecutionApi();
		MatlabScriptInput input = new MatlabScriptInput();
		String startupScriptPath = step.getStartupScriptPath();
		if (startupScriptPath != null) {
			String scriptDirectory;
			String scriptName;
			List<String> parts = Util.extractSpaceSeparatedParts(startupScriptPath);
			String scriptPath = parts.remove(0);
			// the remaining things are arguments, may be an empty list
			List<Object> inArgs = new ArrayList<>(parts);
			// we need to resolve an absolute path to use in Matlab
			if (isPathAbsolute(scriptPath)) {
				scriptName = getFileName(scriptPath, true);
				scriptDirectory = scriptPath.replace(scriptName, "");
			} else {
//				FilePath resolvedScriptPath = resolveInAgentWorkspace(scriptPath);
//				scriptName = resolvedScriptPath.getBaseName(); // baseName: file name w/o extension
//				scriptDirectory = resolvedScriptPath.getParent().getRemote();
				File scriptFile = Paths.get(scriptPath).toFile();
				scriptName = scriptFile.getName();
				scriptDirectory = scriptFile.getParentFile().getAbsolutePath();
				
			}

			// add script parent directory to ml path
			input = new MatlabScriptInput().scriptName("addpath").inArgs(Arrays.asList(scriptDirectory)).outArgs(0);
			mlApi.executeMatlabScriptShort(input); // short -> finishes instantly

			// call script: long -> need to query result
			input = new MatlabScriptInput().scriptName(scriptName).inArgs(inArgs).outArgs(0);
			Job job = mlApi.executeMatlabScriptLong(input);
			HttpRequester.waitForCompletion(job.getJobID());
		}
	}

	
	protected Object getResponse() {
		return this.dataTransferObject.response;
	}
	protected Object response(Object o) {
		this.dataTransferObject.response = String.valueOf(o);
		return this.dataTransferObject.response;
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
					((TestStep) this.dataTransferObject.reportingStep).addArgument(argName, value.toString());
				}
			}
		}
	}

	protected void createEmptyProfile() {
		try {
			new ProfilesApi().createProfile(true);
		} catch (ApiException ignored) {
			// ignored, this is often slow and throws a SocketTimeoutException
		}
	}
	
	protected void openProfile(String profilePath) throws ApiException {
		try {
			new ProfilesApi().openProfile(profilePath, true);
		} catch (ApiException e) {
			// ignored, this is often slow and throws a SocketTimeoutException
			if (!(e.getCause() instanceof SocketTimeoutException)) {
				throw e;
			}
		}
	}
	


	private boolean isUnix(StepContext context) {
		try {
			return context.get(Computer.class).isUnix();
		} catch (Exception e) {
			e.printStackTrace(logger());
		}
		return false;
	}

	protected PrintStream logger() {
		if (localLogger == null) {
			try {
				localLogger = new PrintStream(remoteLogger, true, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		return localLogger;
	}
}
