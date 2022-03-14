package com.btc.ep.plugins.embeddedplatform.util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.openapitools.client.ApiException;
import org.openapitools.client.api.PreferencesApi;
import org.openapitools.client.model.Preference;

public class CompilerHelper {

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

	public static final List<String> KNOWN_COMPILER_PREFERENCE_VALUES = Arrays.asList(
			PREF_COMPILER_GCC_VALUE, PREF_COMPILER_MINGW_VALUE, PREF_COMPILER_MSVC170_VALUE,
			PREF_COMPILER_MSVCPP170_VALUE, PREF_COMPILER_MSVC160_VALUE, PREF_COMPILER_MSVCPP160_VALUE,
			PREF_COMPILER_MSVC150_VALUE, PREF_COMPILER_MSVCPP150_VALUE, PREF_COMPILER_MSVC130_VALUE,
			PREF_COMPILER_MSVC120_VALUE, PREF_COMPILER_MSSDK71_VALUE);
	
	public static String getCompilerPreferenceValue(String compilerShortName) {
		String shortName = compilerShortName.toUpperCase();
		if (shortName.equals("MINGW64")) {
			return PREF_COMPILER_MINGW_VALUE;
		} else if (shortName.equals("MSSDK71")) {
			return PREF_COMPILER_MSSDK71_VALUE;
		} else if (shortName.startsWith("MSVC")) {
			return shortName + "(64bit)";
		}
		// fallback, this might cause an error when no compiler preference value exists
		// by this name
		return compilerShortName;
	}

	/**
	 * Tries to set an arbitrary compiler. Returns the compiler preference value
	 * (e.g. "MinGW64 (64bit)") if succesful or null if no compiler could be set.
	 *
	 * @return compiler preference value or null (if unsuccessful)
	 */
	public static String setArbitraryCompiler() {
		Preference preference = new Preference().preferenceName(PREF_COMPILER_KEY);
		PreferencesApi prefApi = new PreferencesApi();
		for (String compilerPrefValue : KNOWN_COMPILER_PREFERENCE_VALUES) {
			try {
				preference.setPreferenceValue(compilerPrefValue);
				prefApi.setPreferences(Arrays.asList(preference));
				return compilerPrefValue;
			} catch (ApiException ignored) {
				// all unavailable compilers will throw this exception
			}
		}
		return null;
	}

	/**
	 * Set a compiler based on a given shortName.
	 * 
	 * @throws ApiException if the selected compiler is not available
	 */
	public static void setCompiler(String shortName) throws ApiException {
		Preference preference = new Preference().preferenceName(PREF_COMPILER_KEY);
		PreferencesApi prefApi = new PreferencesApi();
		String compilerPreferenceValue = getCompilerPreferenceValue(shortName);
		preference.setPreferenceValue(compilerPreferenceValue);
		prefApi.setPreferences(Arrays.asList(preference));
	}

	/**
	 * Sets the specified compiler. Alternatively, tries to set an arbitrary
	 * fallback compiler.
	 * 
	 * @throws RuntimeException if no compiler can be set.
	 */
	public static void setCompilerWithFallback(String compilerShortName, PrintStream printStream)
			throws RuntimeException {
		String arbitraryCompiler = "n.a.";
		if (compilerShortName != null) {
			try {
				setCompiler(compilerShortName);
			} catch (ApiException e) {
				arbitraryCompiler = setArbitraryCompiler();
				if (arbitraryCompiler != null) {
					printStream.println("The selected compiler '" + compilerShortName
							+ "' could not be set. Falling back to arbitrary compiler '" + arbitraryCompiler + "'.");
				}
			}
		} else {
			arbitraryCompiler = setArbitraryCompiler();
		}
		if (arbitraryCompiler == null) {
			throw new RuntimeException(
					"No compiler could not be set. Please verify if a supported compiler is installed (see BTC EmbeddedPlatform Install Guide)");
		}
	}
	
}
