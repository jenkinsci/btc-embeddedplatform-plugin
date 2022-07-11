package com.btc.ep.plugins.embeddedplatform.reporting;

import java.io.Serializable;

import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper;
import com.btc.ep.plugins.embeddedplatform.util.JUnitXMLHelper.Status;

public class JUnitXmlTestCase implements Serializable {

	public JUnitXmlTestCase(String testCaseName, Status testStatus, String comment) {
		this.name = testCaseName;
		this.status = testStatus;
		this.message = comment;
	}
	private static final long serialVersionUID = 1L;
	public String name;
	public JUnitXMLHelper.Status status;
	public String message;
}
