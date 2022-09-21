package com.btc.ep.plugins.embeddedplatform.reporting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JUnitXmlTestSuite implements Serializable {

	private static final long serialVersionUID = 1L;
	public String suiteName;
	public List<JUnitXmlTestCase> testCases;
	
	public JUnitXmlTestSuite(String testSuiteName) {
		this.suiteName = testSuiteName;
		this.testCases = new ArrayList<>();
	}
	
	public JUnitXmlTestSuite(String testSuiteName, List<JUnitXmlTestCase> testCases) {
		this.suiteName = testSuiteName;
		this.testCases = testCases;
	}
	
}
