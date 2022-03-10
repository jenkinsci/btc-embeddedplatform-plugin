package com.btc.ep.plugins.embeddedplatform.util;

import java.util.Arrays;
import java.util.List;

public class Constants {

	public static final String PREF_COMPILER_KEY = "GENERAL_COMPILER_SETTING";
	public static final String PREF_COMPILER_GCC_VALUE = "GCC64 (64bit)";
	public static final String PREF_COMPILER_MINGW_VALUE = "MinGW64 (64bit)";
	public static final String PREF_COMPILER_MSSDK71_VALUE = "MSSDK71 (64bit)";
	public static final String PREF_COMPILER_MSVC120_VALUE = "MSVC120 (64bit)";
	public static final String PREF_COMPILER_MSVC130_VALUE = "MSVC130 (64bit)";
	public static final String PREF_COMPILER_MSVC150_VALUE = "MSVC150 (64bit)";
	public static final String PREF_COMPILER_MSVCPP150_VALUE = "MSVCPP150 (64bit)";
	public static final String PREF_COMPILER_MSVC160_VALUE = "MSVC160 (64bit)";
	public static final String PREF_COMPILER_MSVCPP160_VALUE = "MSVCPP160 (64bit)";
	public static final String PREF_COMPILER_MSVC170_VALUE = "MSVC170 (64bit)";
	public static final String PREF_COMPILER_MSVCPP170_VALUE = "MSVCPP170 (64bit)";

	public static List<String> getKnownCompilerPreferenceValues() {
		return Arrays.asList(PREF_COMPILER_GCC_VALUE, PREF_COMPILER_MINGW_VALUE, PREF_COMPILER_MSVC170_VALUE,
				PREF_COMPILER_MSVCPP170_VALUE, PREF_COMPILER_MSVC160_VALUE, PREF_COMPILER_MSVCPP160_VALUE,
				PREF_COMPILER_MSVC150_VALUE, PREF_COMPILER_MSVCPP150_VALUE, PREF_COMPILER_MSVC130_VALUE,
				PREF_COMPILER_MSVC120_VALUE, PREF_COMPILER_MSSDK71_VALUE);
	}

	public static final String PREF_MATLAB_VERSION_KEY = "GENERAL_MATLAB_CUSTOM_VERSION";

	public static final String PREF_MATLAB_INSTANCE_POLICY_KEY = "GENERAL_MATLAB_NEW_INSTANCE_POLICY";
	public static final String PREF_MATLAB_INSTANCE_POLICY_AUTO = "AUTO";
	public static final String PREF_MATLAB_INSTANCE_POLICY_ALWAYS = "ALWAYS";
	public static final String PREF_MATLAB_INSTANCE_POLICY_NEVER = "NEVER";

	public static final String PREF_MATLAB_VERSION_POLICY_KEY = "GENERAL_MATLAB_VERSION";
	public static final String PREF_MATLAB_VERSION_POLICY_SYSTEM = "SYSTEM";
	public static final String PREF_MATLAB_VERSION_POLICY_LATEST = "LATEST";
	public static final String PREF_MATLAB_VERSION_POLICY_CUSTOM = "CUSTOM";

}
