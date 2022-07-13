package com.btc.ep.plugins.embeddedplatform.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.btc.ep.plugins.embeddedplatform.reporting.JUnitXmlTestCase;

import hudson.FilePath;


public class JUnitXMLHelper {
	public enum Status {PASSED, FAILED, ERROR, SKIPPED}
	
	private static Map<String, List<JUnitXmlTestCase>> suites;
	private static Map<String, Integer> passedTests;
	private static Map<String, Integer> failedTests;
	private static Map<String, Integer> errorTests;
	private static Map<String, Integer> skippedTests;
	
	public static void initialize() {
		suites = new HashMap<>();
		passedTests = new HashMap<>();
		failedTests = new HashMap<>();
		errorTests = new HashMap<>();
		skippedTests = new HashMap<>();
	}
	
	public static int addSuite(String suitename) {
		if (suites.containsKey(suitename)) {
			return -1; // key already contained
		}
		List<JUnitXmlTestCase> l = new ArrayList<>();
		suites.put(suitename, l);
		passedTests.put(suitename, 0);
		failedTests.put(suitename, 0);
		errorTests.put(suitename, 0);
		skippedTests.put(suitename, 0);
		return 0;
	}
	
	public static int addTest(String suitename, String testname, Status testStatus, String message) {
		List<JUnitXmlTestCase> list = suites.get(suitename);
		/*if (list == null) {
			return -1;
		}*/
		JUnitXmlTestCase TC = new JUnitXmlTestCase(testname, testStatus, message);
		list.add(TC);
		suites.put(suitename, list);
		switch (testStatus) {
			case PASSED:
				passedTests.put(suitename, passedTests.get(suitename)+1);
				break;
			case FAILED:
				failedTests.put(suitename, failedTests.get(suitename)+1);
				break;
			case ERROR:
				errorTests.put(suitename, errorTests.get(suitename)+1);
				break;
			case SKIPPED:
				skippedTests.put(suitename, skippedTests.get(suitename)+1);
				break;
		}
		return 0;
	}
	
	public static void dumpToFile(FilePath file) throws Exception {
		String txt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuites>\n";
		for(String suitename: suites.keySet()) {
			// 2 indent
			txt += "  <testsuite name=\""+suitename + "\" tests=\"" + (
					passedTests.get(suitename) + failedTests.get(suitename) + 
					errorTests.get(suitename) + skippedTests.get(suitename))
					+ "\" errors=\"" + errorTests.get(suitename) +
					"\" failures= \"" + failedTests.get(suitename) + "\">\n";
			List<JUnitXmlTestCase> testCases = suites.get(suitename);
			for(JUnitXmlTestCase tc : testCases) {
				// 4 indent
				txt += "    <testcase name=\"" + tc.name + "\" status=\"" + tc.status + "\">";
				// if we're in a failed test case, add extra layer. 6 indent.
				switch(tc.status) {
					case FAILED:
					case ERROR:
					case SKIPPED:
						txt += "\n      <" + tc.status + " message=\"" + tc.message+"\"/>\n";
					default:
						break;
				}
				txt += "</testcase>\n";
			}
			txt += "  </testsuite>\n";
		}
		txt += "</testsuites>\n";
		System.out.println(txt);
		file.write(txt, StandardCharsets.UTF_8.toString());
	}
	
}
