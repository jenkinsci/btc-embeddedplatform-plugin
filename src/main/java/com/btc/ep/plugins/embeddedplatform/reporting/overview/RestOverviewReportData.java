package com.btc.ep.plugins.embeddedplatform.reporting.overview;

import java.util.List;

public class RestOverviewReportData {

	private String path;
	private List<RestOverviewReportProjectData> projects;

	/**
	 * Get path.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set path.
	 * 
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get projects.
	 * 
	 * @return the projects
	 */
	public List<RestOverviewReportProjectData> getProjects() {
		return projects;
	}

	/**
	 * Set projects.
	 * 
	 * @param projects the projects to set
	 */
	public void setProjects(List<RestOverviewReportProjectData> projects) {
		this.projects = projects;
	}

}
