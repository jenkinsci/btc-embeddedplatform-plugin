package com.btc.ep.plugins.embeddedplatform.reporting;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.btc.ep.plugins.embeddedplatform.reporting.overview.OverviewReport;
import com.btc.ep.plugins.embeddedplatform.reporting.overview.OverviewReportSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReportSection;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.FilePath;

/**
 * This class implements the {@link ReportService} interface for the Jenkins
 * Test Automation Report which shows an overview of all executed steps, the
 * used parameters and links to available sub-reports (e.g. Test Execution, B2B,
 * CodeAnalysis, etc.).
 * 
 * @author thabok
 */
public class ReportService {

	private static final String REL_PATH_JENKINS_REPORT_TEMPLATES = "reporting/templates/jenkinsAutomation";

	private static ReportService instance;

	public static ReportService getInstance() {
		if (instance == null) {
			instance = new ReportService();
			// un-comment this if you want to see messages from freemarker
			// BasicConfigurator.configure();
		}
		return instance;
	}

	// LOGGER
	private static Logger logger = LoggerFactory.getLogger(ReportService.class);

	public File generateProjectReport(JenkinsAutomationReport reportData) {
		File templateFile;
		try {
			templateFile = Util.getResourceAsFile(getClass(),
					REL_PATH_JENKINS_REPORT_TEMPLATES + "/" + reportData.getTemplateFile());

			Map<String, Object> reportModel = reportData.getReportModel();
			Path mainReportPath = templateFile.toPath();

			reportModel.put("page_title", reportData.getSectionName());
			reportModel.put("creator", System.getProperty("user.name"));

			HtmlReportCreator creator = new HtmlReportCreator();
			HtmlReportConfig config = new HtmlReportConfig(ReportType.JENKINS_AUTOMATION_REPORT.toString(), creator)
					.mainTemplate(mainReportPath).mainModel(reportModel).name(reportData.getName());

			for (JenkinsAutomationReportSection section : reportData.getAllSections()) {
				File sectionFile = Util.getResourceAsFile(getClass(),
						REL_PATH_JENKINS_REPORT_TEMPLATES + "/" + section.getTemplateFile());
				config.addTemplateForMainData(sectionFile.toPath());
			}
			File report = config.create();
			return report;

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Test Automation report generation failed. Please see the log file for details.";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}
	}

	public File generateOverviewReport(OverviewReport reportData) {
		File templates;
		try {
			templates = Util.getResourceAsFile(getClass(), REL_PATH_JENKINS_REPORT_TEMPLATES);

			Map<String, Object> reportModel = reportData.getReportModel();
			Path mainReportPath = Paths.get(templates.getAbsolutePath(), reportData.getTemplateFile());

			reportModel.put("page_title", reportData.getSectionName());
			reportModel.put("creator", System.getProperty("user.name"));

			HtmlReportCreator creator = new HtmlReportCreator();
			HtmlReportConfig config = new HtmlReportConfig(ReportType.JENKINS_OVERVIEW_REPORT.toString(), creator)
					.mainTemplate(mainReportPath).mainModel(reportModel).name(reportData.getName());

			for (OverviewReportSection section : reportData.getAllSections()) {
				config.addTemplateForMainData(Paths.get(templates.getAbsolutePath(), section.getTemplateFile()));
			}
			File report = config.create();
			report.deleteOnExit();
			return report;

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Test Automation Overview report generation failed. Please see the log file for details.";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}

	}

	public void exportReport(File report, String destinationFolder, FilePath workspace) throws Exception {
		// transfer local file content into remote object
		FilePath destFile = workspace.child(destinationFolder + "/" + report.getName());
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(report);
			Util.transfer(fis, destFile.write(), true);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		// delete local file
		report.delete();
	}

}
