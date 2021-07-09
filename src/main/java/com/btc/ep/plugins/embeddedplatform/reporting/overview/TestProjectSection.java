package com.btc.ep.plugins.embeddedplatform.reporting.overview;

/**
 * This class defines the main section for the test steps in the overall Test Automation Report.
 * 
 * @author thabok
 */
public class TestProjectSection extends OverviewReportSection {

    public String TEMPLATE = "overviewTestProjects";

    public TestProjectSection() {
        sectionName = "Test Projects";
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        isDefaultClosed = false;
    }

    public TestProjectSection(OverviewReportSection section) {
        this.setProfileData(section.getProfileData());
        this.setProjects(section.getProjects());

        sectionName = "Test Projects";
        templateFile = TEMPLATE + ".html";
        templateVariable = TEMPLATE;
        isDefaultClosed = false;
    }

    public void addProject(TestProject testProject) {
        testProject.setIndex(projects.size());
        projects.add(testProject);
    }

}
