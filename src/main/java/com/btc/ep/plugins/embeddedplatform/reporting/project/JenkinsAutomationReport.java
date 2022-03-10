package com.btc.ep.plugins.embeddedplatform.reporting.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines the overall Test Automation Report. It mainly consists of
 * {@link JenkinsAutomationReportSection}s (most importantly
 * {@link TestStepSection}s) and some metadata.
 * 
 * @author thabok
 */
public class JenkinsAutomationReport extends JenkinsAutomationReportSection {

	private String TEMPLATE = "report";
	Map<String, Object> reportModel = new HashMap<>();
	List<JenkinsAutomationReportSection> sections = new ArrayList<>();

	public JenkinsAutomationReport() {
		reportModel.put(TEMPLATE, this);
		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Test Automation Report";
	}

	public void setSections(List<JenkinsAutomationReportSection> sections) {
		this.sections = sections;
	}

	public void addSection(JenkinsAutomationReportSection section) {
		String templateVariable = section.getTemplateVariable();
		sections.add(section);
		reportModel.put(templateVariable, section);
	}

	public Map<String, Object> getReportModel() {
		return reportModel;

	}

	public String getTemplateFile() {
		return TEMPLATE + ".html";
	}

	public List<JenkinsAutomationReportSection> getAllSections() {
		return sections;

	}

	public String getName() {
		return "TestAutomationReport";

	}

	public void setStartDate(String startDate) {
		reportModel.put("start_date", startDate);
	}

	public void setEndDate(String endDate) {
		reportModel.put("end_date", endDate);
	}

	public String getStartDate() {
		return (String) reportModel.get("start_date");
	}

	public String getEndDate() {
		return (String) reportModel.get("end_date");
	}

	public void setDuration(String durationString) {
		reportModel.put("duration", durationString);
	}

}
