package com.btc.ep.plugins.embeddedplatform.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hudson.FilePath;


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
	public static void dumpToFile(FilePath file) throws Exception {
		String txt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuites>\n";
		for(String suitename: suites.keySet()) {
			// 2 indent
			txt += "  <testsuite name=\""+suitename + "\" tests=\"" + (
					passedTests.get(suitename) + failedTests.get(suitename) + 
					errorTests.get(suitename) + skippedTests.get(suitename))
					+ "\" errors=\"" + errorTests.get(suitename) +
					"\" failures= \"" + failedTests.get(suitename) + "\">\n";
			List<TestCase> testCases = suites.get(suitename);
			for(TestCase tc : testCases) {
				// 4 indent
				txt += "    <testcase name=\"" + tc.name + "\" status=\"" + tc.status + "\">\n";
				// if we're in a failed test case, add extra layer. 6 indent.
				switch(tc.status) {
					case FAILED:
					case ERROR:
					case SKIPPED:
						txt += "      <" + tc.status + " message=\"" + tc.message+"\"/>\n";
				default:
					break;
				}
			}
			txt += "  </testsuite>\n";
		}
		txt += "</testsuites>\n";
		System.out.println(txt);
		file.write(txt, StandardCharsets.UTF_8.toString());
	}
}
