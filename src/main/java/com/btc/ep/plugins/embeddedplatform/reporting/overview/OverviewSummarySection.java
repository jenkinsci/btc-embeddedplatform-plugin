package com.btc.ep.plugins.embeddedplatform.reporting.overview;

/**
 * This class defines a section of the overall Test Automation Report dedicated
 * for profile and environment meta data.
 * 
 * @author thabok
 */
public class OverviewSummarySection extends OverviewReportSection {

	public String TEMPLATE = "overviewSummary";

	private String overallStatus;
	private String noTotal;
	private String noPassed;
	private String noFailed;
	private String totalDuration;
	private String avgDuration;
	private boolean passed;

	public OverviewSummarySection() {
		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Summary";
		isDefaultClosed = false;
	}

	public OverviewSummarySection(OverviewReportSection section) {
		this.setProfileData(section.getProfileData());

		templateFile = TEMPLATE + ".html";
		templateVariable = TEMPLATE;
		sectionName = "Summary";
		isDefaultClosed = false;
	}

	/**
	 * Get overallStatus.
	 * 
	 * @return the overallStatus
	 */
	public String getOverallStatus() {
		return overallStatus;
	}

	/**
	 * Set overallStatus.
	 * 
	 * @param overallStatus the overallStatus to set
	 */
	public void setOverallStatus(String overallStatus) {
		this.overallStatus = overallStatus;
	}

	/**
	 * Get noTotal.
	 * 
	 * @return the noTotal
	 */
	public String getNoTotal() {
		return noTotal;
	}

	/**
	 * Set noTotal.
	 * 
	 * @param noTotal the noTotal to set
	 */
	public void setNoTotal(String noTotal) {
		this.noTotal = noTotal;
	}

	/**
	 * Get noPassed.
	 * 
	 * @return the noPassed
	 */
	public String getNoPassed() {
		return noPassed;
	}

	/**
	 * Set noPassed.
	 * 
	 * @param noPassed the noPassed to set
	 */
	public void setNoPassed(String noPassed) {
		this.noPassed = noPassed;
	}

	/**
	 * Get noFailed.
	 * 
	 * @return the noFailed
	 */
	public String getNoFailed() {
		return noFailed;
	}

	/**
	 * Set noFailed.
	 * 
	 * @param noFailed the noFailed to set
	 */
	public void setNoFailed(String noFailed) {
		this.noFailed = noFailed;
	}

	/**
	 * Get totalDuration.
	 * 
	 * @return the totalDuration
	 */
	public String getTotalDuration() {
		return totalDuration;
	}

	/**
	 * Set totalDuration.
	 * 
	 * @param totalDuration the totalDuration to set
	 */
	public void setTotalDuration(String totalDuration) {
		this.totalDuration = totalDuration;
	}

	/**
	 * Get avgDuration.
	 * 
	 * @return the avgDuration
	 */
	public String getAvgDuration() {
		return avgDuration;
	}

	/**
	 * Set avgDuration.
	 * 
	 * @param avgDuration the avgDuration to set
	 */
	public void setAvgDuration(String avgDuration) {
		this.avgDuration = avgDuration;
	}

	/**
	 * Get passed.
	 * 
	 * @return the passed
	 */
	public boolean isPassed() {
		return passed;

	}

	/**
	 * Set passed.
	 * 
	 * @param passed the passed to set
	 */
	public void setPassed(boolean passed) {
		this.passed = passed;

	}

}
