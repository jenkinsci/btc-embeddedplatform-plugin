package com.btc.ep.plugins.embeddedplatform.reporting.project;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows to serialize a report in order to continue with the data another time. Essential for the
 * Migration Suite scenario where not all test steps are running in the same application life cycle (or even on the same
 * PC).
 * 
 * @author thabok
 */
public class SerializableReportingContainer extends JenkinsAutomationReportSection {

    private List<JenkinsAutomationReportSection> sections = new ArrayList<>();
    private String startDate;
    private String endDate;
    private String duration;

    /**
     * Get sections.
     * 
     * @return the sections
     */
    public List<JenkinsAutomationReportSection> getSections() {
        return sections;
    }

    /**
     * Set sections.
     * 
     * @param sections the sections to set
     */
    public void setSections(List<JenkinsAutomationReportSection> sections) {
        this.sections = sections;
    }

    /**
     * Get startDate.
     * 
     * @return the startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Set startDate.
     * 
     * @param startDate the startDate to set
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * Get endDate.
     * 
     * @return the endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Set endDate.
     * 
     * @param endDate the endDate to set
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * Get duration.
     * 
     * @return the duration
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Set duration.
     * 
     * @param duration the duration to set
     */
    public void setDuration(String duration) {
        this.duration = duration;
    }

}
