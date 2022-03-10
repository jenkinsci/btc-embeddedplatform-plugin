package com.btc.ep.plugins.embeddedplatform.reporting.project;

/**
 * This class defines a section of the overall Test Automation Report dedicated
 * for test step arguments. Any step parameter that is specified in the
 * Jenkinsfile will end up in this section of the report.
 * 
 * @author thabok
 */
public class StepArgSection extends JenkinsAutomationReportSection {

	public String TEMPLATE = "reportStepArguments";

	public StepArgSection() {
		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Test Automation Arguments";
	}

	public StepArgSection(JenkinsAutomationReportSection section) {
		this.setProfileData(section.getProfileData());
		this.setSteps(section.getSteps());

		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Test Automation Arguments";
	}

	public void addStep(TestStep step) {
		step.setIndex(steps.size());
		this.steps.add(step);
	}

}
