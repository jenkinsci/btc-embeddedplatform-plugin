package com.btc.ep.plugins.embeddedplatform.reporting.project;

/**
 * This class defines a section of the overall Test Automation Report dedicated
 * for profile and environment meta data.
 * 
 * @author thabok
 */
public class MetaInfoSection extends JenkinsAutomationReportSection {

	public String TEMPLATE = "reportMetaInformation";

	public MetaInfoSection() {
		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Meta Information";
	}

	public MetaInfoSection(JenkinsAutomationReportSection section) {
		this.setProfileData(section.getProfileData());
		this.setSteps(section.getSteps());

		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Meta Information";
	}

}
