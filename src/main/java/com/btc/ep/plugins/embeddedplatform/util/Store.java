package com.btc.ep.plugins.embeddedplatform.util;

import java.nio.file.Path;
import java.util.Date;

import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.MetaInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.PilInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.StepArgSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;

import hudson.FilePath;
import hudson.Proc;

public class Store {

	// General
	public static Proc epProcess;
	public static Process epProcess2;
	public static FilePath epp;
	public static Path epp2;
	public static String exportPath;
	public static String globalSuffix;

	// Reporting
	public static JenkinsAutomationReport reportData;
	public static TestStepSection testStepSection;
	public static PilInfoSection pilInfoSection;
	public static StepArgSection testStepArgumentSection;
	public static Date startDate;
	public static MetaInfoSection metaInfoSection;
	public static String matlabVersion;

}
