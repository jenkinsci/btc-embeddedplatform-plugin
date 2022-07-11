package com.btc.ep.plugins.embeddedplatform.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.openapitools.client.ApiException;

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.reporting.ReportService;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;

import htmlpublisher.HtmlPublisher;
import htmlpublisher.HtmlPublisherTarget;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.junit.pipeline.JUnitResultsStep;

public class StepExecutionHelper {

	
	public static void postProcessing(DataTransferObject dto) {
		// Adapt updated data where needed
		if (dto.epp != null && !dto.epp.equals(Store.epp2)) {
			Store.epp2 = dto.epp;
		}
		if (dto.exportPath != null && !dto.exportPath.equals(Store.exportPath)) {
			Store.exportPath = dto.exportPath;
		}
		
		// Reporting
		if (!dto.reportingDisabled && dto.startTime != null) {
			checkArgument(dto.reportingStep.getDetails().size() == dto.reportingStep.getDetailsLinks().size(),
					"Reporting: All details must have a corresponding link.");
		
			dto.reportingStep.setStatusOK(dto.status == Status.OK);
			dto.reportingStep.setStatusWARNING(dto.status == Status.WARNING);
			dto.reportingStep.setTime(Util.getTimeDiffAsString(dto.startTime, new Date()));
			Store.testStepSection.addStep(dto.reportingStep);
		}
	}

	/**
	 * Invokes the execution of the BtcExecution::call() method which triggers performAction() on the extending class.
	 * 
	 * @param exec the object defining the current step execution
	 * @param context the execution context from Jenkins
	 * @return a DataTransferObject that can be used to send back data to the main thread running on the Jenkins controller (use this for postprocessing)
	 */
	public static DataTransferObject executeOnAgent(BtcExecution exec, StepContext context) {
		try {
			Launcher launcher = context.get(Launcher.class);
			if (launcher != null) {
				VirtualChannel channel = launcher.getChannel();
				if (channel == null) {
					throw new IllegalStateException("Launcher doesn't support remoting but it is required");
				}
				return channel.call(exec);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PrintStream getLogger(StepContext context) {
		PrintStream logger = null;
		try {
			logger = context.get(TaskListener.class).getLogger();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return logger;
	}
	
	public static void exportJUnitReport(StepContext context) throws Exception {
		JUnitXMLHelper.dumpToFile(context.get(FilePath.class).child("JUnit.xml"));
		JUnitResultsStep junitStep = new JUnitResultsStep("JUnit.xml");
		junitStep.setAllowEmptyResults(true);
		junitStep.start(context).start();
	}
	
	public static void assembleProjectReport(StepContext context, PrintStream logger) {
		try {
			// TODO: generate and export profile messages report and add it to the main
			// report EP-2539
			Store.reportData.addSection(Store.testStepSection);
			Store.testStepArgumentSection.setSteps(Store.testStepSection.getSteps());
			Store.reportData.addSection(Store.testStepArgumentSection);
			Store.reportData.addSection(Store.pilInfoSection);
			String endDate = Util.DATE_FORMAT.format(new Date());
			Store.reportData.setEndDate(endDate);
			String durationString = Util.getTimeDiffAsString(new Date(), Store.startDate);
			Store.reportData.setDuration(durationString);
			File report = ReportService.getInstance().generateProjectReport(Store.reportData);
			try {
				ReportService.getInstance().exportReport(report, Store.exportPath, context.get(FilePath.class));
			} catch (IOException e) {
				throw new IOException("Failed to export project report to " + Store.exportPath + ": " + e.getMessage());
			}
			// Report file name must match report value of JenkinsAutomationReport
			// constructor
			publishHtml(context, "Test Automation Report", "TestAutomationReport.html");
			archiveArtifacts(context);
		} catch (Exception e) {
			warning(logger, "Failed to create the project report.", e);
		}
	}
	
	/**
	 * Uses the HTML Publisher Plugin to publish an HTML file in Jenkins. This makes
	 * it available on the Job page for easy access.
	 * 
	 * @param reportTitle the report's title
	 * @param fileName    name of the file to publish
	 */
	private static void publishHtml(StepContext context, String reportTitle, String fileName) throws Exception {
		String reportDir = Store.exportPath.replace("\\", "/");
		HtmlPublisherTarget target = new HtmlPublisherTarget(reportTitle, reportDir, fileName, true, // <-- store all
																										// for builds,
																										// not just
																										// latest
				true, // <-- link should reference last build, not last successful
				true); // <-- build should not fail if the html file is not present
		HtmlPublisher.publishReports(context.get(Run.class), context.get(FilePath.class),
				context.get(TaskListener.class), Collections.singletonList(target), HtmlPublisher.class);
	}
	
	private static void archiveArtifacts(StepContext context) throws Exception {
		Run<?,?> build = context.get(Run.class);
		FilePath workspace = context.get(FilePath.class);
		Launcher launcher = context.get(Launcher.class);
		TaskListener taskListener = context.get(TaskListener.class);
		
		Path wsPath = Paths.get(workspace.getRemote());
		Path eppPath = Paths.get(Store.epp.getRemote());
		Path relPath = wsPath.relativize(eppPath);
		
		ArtifactArchiver aa = new ArtifactArchiver(relPath.toString());
		aa.setAllowEmptyArchive(true);
		
		aa.perform(build, workspace, launcher, taskListener);
	}
	
	/**
	 * Logs the given message and prints the stack trace.
	 */
	private static void warning(PrintStream logger, String message, Throwable t) {
		log(logger, "Warning: " + message);
		if (t != null) {
			// print response if available
			if (t instanceof ApiException) {
				log(logger, ((ApiException) t).getResponseBody());
			}
			// print full stack trace
			try {
				t.printStackTrace(logger);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Writes the given message to the jenkins console output. All messages are
	 * prefixed with "[BTC] "
	 * 
	 * @param message
	 */
	public static void log(PrintStream logger, String message) {
		logger.print("[BTC] ");
		logger.println(message);
	}
	
	/**
	 * Writes the given message to the jenkins console output. The message is
	 * formatted with the given args: String.format(message, args) All messages are
	 * prefixed with "[BTC] "
	 * 
	 * @param message
	 */
	public static void log(String message, Object... formatArgs) {
		log(String.format(message, formatArgs));
	}
	
	public static FilePath resolveInAgentWorkspace(StepContext context, String relOrAbsPathInWorkspace) throws Exception {
		return context.get(FilePath.class).child(relOrAbsPathInWorkspace);
	}
}
