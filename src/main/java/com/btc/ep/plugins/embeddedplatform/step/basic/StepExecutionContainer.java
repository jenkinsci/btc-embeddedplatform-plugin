package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.Serializable;
import java.util.Date;

import com.btc.ep.plugins.embeddedplatform.reporting.project.BasicStep;
import com.btc.ep.plugins.embeddedplatform.util.Status;

public class StepExecutionContainer implements Serializable {

	private static final long serialVersionUID = 3619857832442814861L;
	public Status status = Status.OK;
	/**
	 * Response returned to Jenkins (may be overwritten by implementation)
	 */
	public Object response = 200;
	public String functionName;
	public BasicStep reportingStep;
	public Date t1 = null;
	
	public static final String TRUE  = "TRUE";
	public static final String FALSE = "FALSE";


	public boolean reportingDisabled = false;
	
	/**
	 * Prevents the step from being added to the report
	 *
	 */
	public StepExecutionContainer noReporting() {
		this.reportingDisabled = true;
		return this;
	}
}
