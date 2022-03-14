package com.btc.ep.plugins.embeddedplatform.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.Launcher;
import hudson.Proc;

public class ProcessHelper {

	/**
	 * Launches a process with the given command using hudson.Launcher.class, starts
	 * it and returns the process. The process inherits the environment from Jenkins
	 * which allows Jenkins to kill processes when the run finishes/aborts/exits
	 * prematurely, etc.
	 * 
	 * @param command the command to run
	 * @return the process object
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Proc spawnManagedProcess(List<String> command, StepContext stepContext) throws IOException, InterruptedException {
		// Use magical "get(...)" command from context to get the Launcher
		Launcher launcher = stepContext.get(Launcher.class);

		// Use magical "get(...)" command from context to get the set of EnvVars that
		// contains the required cookie
		// This cookie is some sort of hash that jenkins needs to be able to kill a
		// spawned process
		Map<String, String> taskHandle = new HashMap<>();
		String cookie = stepContext.get(hudson.EnvVars.class).get(Constants.JENKINS_NODE_COOKIE);
		taskHandle.put(Constants.JENKINS_NODE_COOKIE, cookie);

		// Start external process with the identifying cookie and return the process
		// object. Important: readStdout is needed if we want to access the stdout InputStream of the process
		Proc process = launcher.launch().cmds(command).envs(taskHandle).quiet(true).readStdout().start();
		return process;
	}
	
}
