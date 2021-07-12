package com.btc.ep.plugins.embeddedplatform.reporting.overview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;

/**
 * This class defines the overall Test Automation Report. It mainly consists of {@link OverviewReportSection}s
 * (most importantly {@link TestStepSection}s) and some metadata.
 * 
 * @author thabok
 */
public class OverviewReport extends OverviewReportSection {

    private String TEMPLATE = "overviewReport";
    Map<String, Object> reportModel = new HashMap<>();
    List<OverviewReportSection> sections = new ArrayList<>();

    public OverviewReport() {
        reportModel.put(TEMPLATE, this);
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        sectionName = "Test Automation Overview Report";
    }

    public void setSections(List<OverviewReportSection> sections) {
        this.sections = sections;
    }

    public void addSection(OverviewReportSection section) {
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

    public List<OverviewReportSection> getAllSections() {
        return sections;

    }

    public String getName() {
        return "TestAutomationOverviewReport";

    }

    public void setStartDate(String startDate) {
        reportModel.put("start_date", startDate);
    }

    public void setEndDate(String endDate) {
        reportModel.put("end_date", endDate);
    }

    public String getStartDate() {
        return (String)reportModel.get("start_date");
    }

    public String getEndDate() {
        return (String)reportModel.get("end_date");
    }

    public void setDuration(String durationString) {
        reportModel.put("duration", durationString);
    }

}
