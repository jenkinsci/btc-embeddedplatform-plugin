package com.btc.ep.plugins.embeddedplatform.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestSuite;
import com.btc.ep.plugins.embeddedplatform.reporting.project.BasicStep;
import com.btc.ep.plugins.embeddedplatform.util.Status;

/**
 * This class is used as a wrapper for anything the step executions want to return from the agent-based execution to the Jenkins controller which manages the global state.
 * @author thabok
 *
 */
public class DataTransferObject implements Serializable {

	private static final long serialVersionUID = 1099810872423184683L;
	
	public Status status = Status.OK;
	public String response; // does this make sense?
	public BasicStep reportingStep;
	public Date startTime;
	public boolean reportingDisabled = false;
	public String epp;
	public String eppName;
	public String exportPath;
	public String matlabVersion;
	public JUnitXmlTestSuite testSuite;
	public Map<String, String> metadata;
	public String messageMarker;
	public String baseDir;


	
	public DataTransferObject() {
		this.startTime = new Date();
	}
	
	public DataTransferObject(Status status, BasicStep reportingStep, String response, Date startTime, boolean reportingDisabled) {
		this.reportingStep = reportingStep;
		this.reportingDisabled = reportingDisabled;
		this.startTime = startTime;
		this.status = status;
		this.response = response;
	}

}