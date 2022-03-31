package com.btc.ep.plugins.embeddedplatform.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class TestCase {
	String name;
	JUnitXMLHelper.Status status;
	String message;
}

public class JUnitXMLHelper {
	public enum Status {PASSED, FAILED, ERROR, SKIPPED}
	
	public static Map<String, List<TestCase>> suites = new HashMap<>();
	private static Map<String, Integer> passedTests = new HashMap<>();
	private static Map<String, Integer> failedTests = new HashMap<>();
	private static Map<String, Integer> errorTests = new HashMap<>();
	private static Map<String, Integer> skippedTests = new HashMap<>();
	
	public static int addSuite(String suitename) {
		if (suites.containsKey(suitename)) {
			return -1; // key already contained
		}
		List<TestCase> l = new ArrayList<>();
		suites.put(suitename, l);
		passedTests.put(suitename, 0);
		failedTests.put(suitename, 0);
		errorTests.put(suitename, 0);
		skippedTests.put(suitename, 0);
		return 0;
	}
	
	public static int addTest(String suitename, String testname, Status testStatus, String message) {
		List<TestCase> list = suites.get(suitename);
		/*if (list == null) {
			return -1;
		}*/
		TestCase TC = new TestCase();
		TC.name = testname;
		TC.message = message;
		TC.status = testStatus;
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
	public static String parse() {
		int allPassed = 0;
		int allFailed = 0;
		int allError = 0;
		int allSkipped = 0;
		for(String suitename: suites.keySet()) {
			allPassed += passedTests.get(suitename);
			allFailed += failedTests.get(suitename);
			allError += errorTests.get(suitename);
			allSkipped += skippedTests.get(suitename);
		}
		int allTests = allPassed + allFailed + allError + allSkipped;
		String txt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuites " + 
				"errors=\"" + String.valueOf(allError + allFailed + allSkipped) + "\" " + 
				"failures=\"" + String.valueOf(allFailed) + "\" " + 
				"tests=\"" + String.valueOf(allTests) + "\"" + 
		">\n";
		for(String suitename: suites.keySet()) {
			// 2 indent
			int num_tests = passedTests.get(suitename) + failedTests.get(suitename) + 
							errorTests.get(suitename) + skippedTests.get(suitename);
			txt += "  <testsuite name=\""+suitename + "\" " + 
						"tests=\"" + String.valueOf(num_tests) + "\" " + 
						"errors=\"" + String.valueOf(errorTests.get(suitename)) + "\" " + 
						"failures= \"" + String.valueOf(failedTests.get(suitename)) + "\">\n";
			List<TestCase> testCases = suites.get(suitename);
			if (testCases != null) {
				for(TestCase TC : testCases) {
					// 4 indent
					txt += "    <testcase name=\"" + TC.name + "\" " + 
								"status=\"" + TC.status.toString() + "\" " + 
								"classname=\"" + "placeholder-class\" >";
					// if we're in a failed test case, add extra layer. 6 indent.
					String msg = TC.message == null ? "" : TC.message;
					switch(TC.status) {
						case FAILED:
							txt += "\n      <failure message=\""+msg+"\"/></failure>\n    ";
							break;
						case ERROR:
							txt += "\n      <error message=\""+msg+"\"/></error>\n    ";
							break;
						case SKIPPED:
							txt += "\n      <skipped message=\""+msg+"\"/>\n    ";
							break;
						default:
							break;
					}
					txt += "</testcase>\n";
				}
			}
			txt += "  </testsuite>\n";
		}
		txt += "</testsuites>\n";
		return txt;
	}
}
