package com.btc.ep.plugins.embeddedplatform.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class TestConfig {

	public static final List<String> INIT_STEPS = Arrays.asList("loadProfile", "cCode", "targetLink", "embeddedCoder", "simulink", "simulinkToplevel");
	
	public GeneralOptions generalOptions;
	public List<Map<String, Object>> testSteps;
	
}
