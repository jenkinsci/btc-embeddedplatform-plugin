package com.btc.ep.plugins.embeddedplatform.reporting.overview;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a basic test step in the overall Test Automation Report.
 * The step has a name, an info attribute, a duration a status and a verdict.
 * The status reflects if there were any errors while the verdict reflects the
 * passed/failed result of test activities. The verdict can be absent for steps
 * that don't provide one.
 * 
 * @author thabok
 */
public class TestProject {

	/*
	 * FIELDS
	 */
	private String profileName = "";
	private String reportLink;
	protected String info = "";
	protected boolean isStatusOK = false;
	protected boolean isStatusWARNING = false;
	protected boolean isPassed = false;
	protected boolean isFailed = false;
	protected boolean isSkipped = false;
	protected String result = "";
	protected int index;
	protected String time = "00:00:00";
	protected List<String> details = new ArrayList<>(1);
	protected List<String> detailsLinks = new ArrayList<>(1);

	/**
	 * Get info.
	 * 
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Set info.
	 * 
	 * @param info the info to set
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * Get isStatusOK.
	 * 
	 * @return the isStatusOK
	 */
	public boolean isStatusOK() {
		return isStatusOK;
	}

	/**
	 * Set isStatusOK.
	 * 
	 * @param isStatusOK the isStatusOK to set
	 */
	public void setStatusOK(boolean isStatusOK) {
		this.isStatusOK = isStatusOK;
	}

	/**
	 * Get isStatusWARNING.
	 * 
	 * @return the isStatusWARNING
	 */
	public boolean isStatusWARNING() {
		return isStatusWARNING;
	}

	/**
	 * Set isStatusWARNING.
	 * 
	 * @param isStatusWARNING the isStatusWARNING to set
	 */
	public void setStatusWARNING(boolean isStatusWARNING) {
		this.isStatusWARNING = isStatusWARNING;
	}

	/**
	 * Get isPassed.
	 * 
	 * @return the isPassed
	 */
	public boolean isPassed() {
		return isPassed;
	}

	/**
	 * Set isPassed.
	 * 
	 * @param isPassed the isPassed to set
	 */
	public void setPassed(boolean isPassed) {
		this.isPassed = isPassed;
	}

	/**
	 * Get isFailed.
	 * 
	 * @return the isFailed
	 */
	public boolean isFailed() {
		return isFailed;
	}

	/**
	 * Set isFailed.
	 * 
	 * @param isFailed the isFailed to set
	 */
	public void setFailed(boolean isFailed) {
		this.isFailed = isFailed;
	}

	/**
	 * Get isSkipped.
	 * 
	 * @return the isSkipped
	 */
	public boolean isSkipped() {
		return isSkipped;
	}

	/**
	 * Set isSkipped.
	 * 
	 * @param isSkipped the isSkipped to set
	 */
	public void setSkipped(boolean isSkipped) {
		this.isSkipped = isSkipped;
	}

	/**
	 * Get result.
	 * 
	 * @return the result
	 */
	public String getResult() {
		return result;
	}

	/**
	 * Set result.
	 * 
	 * @param result the result to set
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * Get index.
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Set index.
	 * 
	 * @param index the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Get time.
	 * 
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * Set time.
	 * 
	 * @param time the time to set
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * Get details.
	 * 
	 * @return the details
	 */
	public List<String> getDetails() {
		return details;
	}

	/**
	 * Set details.
	 * 
	 * @param details the details to set
	 */
	public void setDetails(List<String> details) {
		this.details = details;
	}

	/**
	 * Get detailsLinks.
	 * 
	 * @return the detailsLinks
	 */
	public List<String> getDetailsLinks() {
		return detailsLinks;
	}

	/**
	 * Set detailsLinks.
	 * 
	 * @param detailsLinks the detailsLinks to set
	 */
	public void setDetailsLinks(List<String> detailsLinks) {
		this.detailsLinks = detailsLinks;
	}

	/**
	 * Get profileName.
	 * 
	 * @return the profileName
	 */
	public String getProfileName() {
		return profileName;

	}

	/**
	 * Set profileName.
	 * 
	 * @param profileName the profileName to set
	 */
	public void setProfileName(String profileName) {
		this.profileName = profileName;

	}

	/**
	 *
	 *
	 * @param string
	 */
	public void addDetails(String string) {
		details.add(string);
	}

	/**
	 *
	 *
	 * @param link
	 */
	public void addDetailsLink(String link) {
		detailsLinks.add(link);

	}

	/**
	 * Get reportLink.
	 * 
	 * @return the reportLink
	 */
	public String getReportLink() {
		return reportLink;

	}

	/**
	 * Set reportLink.
	 * 
	 * @param reportLink the reportLink to set
	 */
	public void setReportLink(String reportLink) {
		this.reportLink = reportLink;

	}

}
