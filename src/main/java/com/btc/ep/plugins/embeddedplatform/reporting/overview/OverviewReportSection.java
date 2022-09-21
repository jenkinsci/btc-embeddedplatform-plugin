package com.btc.ep.plugins.embeddedplatform.reporting.overview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class defines a section of the overall Test Automation Report.
 * 
 * @author thabok
 */
public class OverviewReportSection {

	protected String templateFile;
	protected String templateVariable;
	protected String sectionName;
	protected boolean isCollapsible = true;
	protected boolean isDefaultClosed = true;
	protected List<TestProject> projects = new ArrayList<>();
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
	 * Get projects.
	 * 
	 * @return the projects
	 */
	public List<TestProject> getProjects() {
		return projects;

	}

	/**
	 * Set projects.
	 * 
	 * @param projects the projects to set
	 */
	public void setProjects(List<TestProject> projects) {
		this.projects = projects;

	}

}
