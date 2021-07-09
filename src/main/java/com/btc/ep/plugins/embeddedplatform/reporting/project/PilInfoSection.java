package com.btc.ep.plugins.embeddedplatform.reporting.project;

/**
 * This class defines a section of the overall Test Automation Report dedicated for PIL information.
 * 
 * @author thabok
 */
public class PilInfoSection extends JenkinsAutomationReportSection {

    public String TEMPLATE = "reportPilRuntimeInfo";

    public PilInfoSection() {
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        sectionName = "PIL Runtime Information";
    }

    public PilInfoSection(JenkinsAutomationReportSection section) {
        this.setProfileData(section.getProfileData());
        this.setSteps(section.getSteps());

        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        sectionName = "PIL Runtime Information";
    }

}
