package com.btc.ep.plugins.embeddedplatform.util;

import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class MigrationSuiteHelper {

	public static String getExecutionRecordSourceFolderName(String config) {
		return String.format("Source Config Simulation (%s)", config);
	}

	public static String getExecutionRecordTargetFolderName(String config) {
		return String.format("Target Config Simulation (%s)", config);
	}

	private static String getStashName() {
		return getStashName(null);
	}

	private static String getStashName(String suffix) {
		String name = "btc_migration_" + Store.epp.getBaseName() + "_source";
		if (suffix != null && !suffix.isEmpty()) {
			name += "_" + suffix;
		}
		return name;
	}

	public static void stashFiles(String includes, StepContext stepContext) throws Exception {
		String excludes = ""; // don't exclude anything
		StashManager.stash(stepContext.get(Run.class), getStashName(), stepContext.get(FilePath.class),
				stepContext.get(Launcher.class), stepContext.get(EnvVars.class), stepContext.get(TaskListener.class),
				includes, excludes, false, false);
	}

	public static void unstashFiles(StepContext stepContext) throws Exception {
		StashManager.unstash(stepContext.get(Run.class), getStashName(), stepContext.get(FilePath.class),
				stepContext.get(Launcher.class), stepContext.get(EnvVars.class), stepContext.get(TaskListener.class));
	}

}
