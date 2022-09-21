package com.btc.ep.plugins.embeddedplatform.step;

public interface MatlabAwareStep {

	public String getMatlabVersion();
	
	public String getStartupScriptPath();
	
	public default String getMatlabInstancePolicy() {
		return "AUTO";
	}
	
}
