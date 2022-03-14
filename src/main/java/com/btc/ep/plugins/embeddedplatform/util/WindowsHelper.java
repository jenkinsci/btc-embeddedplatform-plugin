package com.btc.ep.plugins.embeddedplatform.util;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import hudson.Proc;

public class WindowsHelper {
	
	/**
	 * Queries the registry to retrieve the install directory of the active EP version
	 * @param stepContext 
	 * @return
	 * @throws Exception
	 */
	public static String getEpInstallPathFromRegistry(StepContext stepContext) throws Exception {
		String psSingleLineCommand = "\"$epVersions = Get-ItemProperty -Path HKLM:\\SOFTWARE\\BTC | Get-ChildItem | ForEach-Object { Write-Output $_.Name.Replace('HKEY_LOCAL_MACHINE', 'HKLM:') };$activeVersionPath = foreach ($epVersion in $epVersions) { Push-Location $epVersion; $isActive = (Get-ItemPropertyValue . 'EPACTIVE'); if ($isActive -eq 1) { Write-Output (Get-ItemPropertyValue . 'Path') } }; Write-Output $activeVersionPath\"";
		
		// Use ProcessBuilder to run the script with powershell
		List<String> cmd = Arrays.asList("powershell", "-Command", psSingleLineCommand);
		Proc proc = ProcessHelper.spawnManagedProcess(cmd, stepContext);
		String epInstallDir = Util.readStringFromStream(proc.getStdout());
		
		// Check feasability (should be something like "C:\Program Files\BTC\ep22.1p0"
		if (epInstallDir == null || !epInstallDir.contains(":")) {
			throw new IllegalStateException("Could not find an active BTC EmbeddedPlatform version in the windows registry.");
		}
		return epInstallDir;
	}
	
}
