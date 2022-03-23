package com.btc.ep.plugins.embeddedplatform.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;


class TestCase {
	String name;
	JUnitXMLHelper.Status status;
	String message;
}

public class JUnitXMLHelper {
	public enum Status {PASSED, FAILED, ERROR, SKIPPED}
	
	public static Map<String, List<TestCase>> suites;
	private static Map<String, Integer> passedTests;
	private static Map<String, Integer> failedTests;
	private static Map<String, Integer> errorTests;
	private static Map<String, Integer> skippedTests;
	
	public static int addSuite(String suitename) {
		if (suites.containsKey(suitename)) {
			return -1; // key already contained
		}
		suites.put(suitename, null);
		passedTests.put(suitename, 0);
		failedTests.put(suitename, 0);
		errorTests.put(suitename, 0);
		skippedTests.put(suitename, 0);
		return 0;
	}
	
	public static int addTest(String suitename, String testname, Status testStatus, String message) {
		List<TestCase> list = suites.get(suitename);
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
	public static void parse(Path filename) {
		String txt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuites>\n";
		for(String suitename: suites.keySet()) {
			// 2 indent
			txt += "  <testsuite name=\""+suitename + "\" tests=\"" + String.valueOf(
					passedTests.get(suitename) + failedTests.get(suitename) + 
					errorTests.get(suitename) + skippedTests.get(suitename)
					) + "\" errors=\"" + String.valueOf(errorTests.get(suitename)) +
					"\" failures= \"" + String.valueOf(failedTests.get(suitename)) + "\">\n";
			List<TestCase> testCases = suites.get(suitename);
			for(TestCase TC : testCases) {
				// 4 indent
				txt += "    <testcase name=\"" + TC.name + "\" status=\"" + TC.status.toString() + "\">\n";
				// if we're in a failed test case, add extra layer. 6 indent.
				switch(TC.status) {
					case FAILED:
					case ERROR:
					case SKIPPED:
						txt += "      <"+TC.status.toString()+ " message=\""+TC.message+"\"/>\n";
				default:
					break;
				}
				txt += "    </testcase>\n";
			}
			txt += "  </testsuite>\n";
		}
		txt += "</testsuites>\n";
		System.out.println(txt);
		try {
			OpenOption[] options = new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW };
			Files.writeString(filename, txt, options);
		} catch (IOException e) {
			// failed to write XML file.
			// TODO: right now we just silently fail
		}
	}
}
