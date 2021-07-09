package com.btc.ep.plugins.embeddedplatform.reporting.project;

/**
 * This class defines the main section for the test steps in the overall Test Automation Report.
 * 
 * @author thabok
 */
public class TestStepSection extends JenkinsAutomationReportSection {

    public String TEMPLATE = "reportTestSteps";

    public TestStepSection() {
        sectionName = "Test Automation Steps";
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        isDefaultClosed = false;
    }

    public TestStepSection(JenkinsAutomationReportSection section) {
        this.setProfileData(section.getProfileData());
        this.setSteps(section.getSteps());

        sectionName = "Test Automation Steps";
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        isDefaultClosed = false;
    }

    public void addStep(BasicStep testStep) {
        testStep.setIndex(steps.size());
        steps.add(testStep);
    }

}
