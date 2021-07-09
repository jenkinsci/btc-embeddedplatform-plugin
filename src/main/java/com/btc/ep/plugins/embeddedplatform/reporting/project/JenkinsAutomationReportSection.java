package com.btc.ep.plugins.embeddedplatform.reporting.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class defines a section of the overall Test Automation Report.
 * 
 * @author thabok
 */
public class JenkinsAutomationReportSection {

    protected String templateFile;
    protected String templateVariable;
    protected String sectionName;
    protected boolean isCollapsible = true;
    protected boolean isDefaultClosed = true;
    protected List<BasicStep> steps = new ArrayList<>();
    protected Map<String, String> profileData;
    protected String worstCaseExecutionTime = "n.a.";
    protected String maxStackSize = "n.a.";

    public String getWorstCaseExecutionTime() {
        return worstCaseExecutionTime;

    }

    public void setWorstCaseExecutionTime(String worstCaseExecutionTime) {
        this.worstCaseExecutionTime = worstCaseExecutionTime;

    }

    public String getMaxStackSize() {
        return maxStackSize;

    }

    public void setMaxStackSize(String maxStackSize) {
        this.maxStackSize = maxStackSize;

    }

    public Map<String, String> getProfileData() {
        return profileData;

    }

    public void setProfileData(Map<String, String> profileData) {
        this.profileData = profileData;

    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public void setTemplateVariable(String templateVariable) {
        this.templateVariable = templateVariable;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public String getTemplateVariable() {
        return templateVariable;
    }

    public String getSectionName() {
        return sectionName;
    }

    public boolean isCollapsible() {
        return isCollapsible;
    }

    public boolean isDefaultClosed() {
        return isDefaultClosed;
    }

    /**
     * Get steps.
     * 
     * @return the steps
     */
    public List<BasicStep> getSteps() {
        return steps;

    }

    /**
     * Set steps.
     * 
     * @param steps the steps to set
     */
    public void setSteps(List<BasicStep> steps) {
        this.steps = steps;

    }

}
