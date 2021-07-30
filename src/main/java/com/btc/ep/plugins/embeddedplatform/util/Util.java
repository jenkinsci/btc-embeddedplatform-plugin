package com.btc.ep.plugins.embeddedplatform.util;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.openapitools.client.ApiException;
import org.openapitools.client.api.PreferencesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.model.Preference;
import org.openapitools.client.model.ProfilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static Logger logger = LoggerFactory.getLogger(Util.class);

    private static final int ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int ONE_HOUR_IN_MILLIS = 1000 * 60 * 60;
    private static final int ONE_MINUTE_IN_MILLIS = 1000 * 60;
    private static final int ONE_SECOND_IN_MILLIS = 1000;

    public static final DateFormat DATE_FORMAT =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault());

    public static Map<Object, Object> toMap(Object... objects) {
        if (objects.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Inconsistent number of key-value pairs (received " + objects.length + " items).");
        }
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < (objects.length - 1); i = i + 2) {
            map.put(objects[i], objects[i + 1]);
        }
        return map;
    }

    /**
     * 
     * Returns the time difference as a String of days, hours, minutes and seconds
     * (00d 00:00:00)
     *
     * @param d1
     * @param d2
     * @return the difference as a string
     */
    public static String getTimeDiffAsString(Date d1, Date d2) {
        long duration = Math.abs(d1.getTime() - d2.getTime());
        return getDurationAsString(duration);
    }

    public static String getDurationAsString(long duration) {
        int days = (int)duration / (ONE_DAY_IN_MILLIS);
        int hours = (int)(duration / (ONE_HOUR_IN_MILLIS)) % 24;
        int minutes = (int)(duration / (ONE_MINUTE_IN_MILLIS)) % 60;
        int seconds = (int)(duration / (ONE_SECOND_IN_MILLIS)) % 60;
        String durationString = (days > 0 ? days + "d" : "");
        durationString += (hours < 10 ? "0" + hours : hours) + ":";
        durationString += (minutes < 10 ? "0" + minutes : minutes) + ":";
        durationString += (seconds < 10 ? "0" + seconds : seconds);
        return durationString;
    }

    public static long durationToMilliseconds(String durationString) {
        long duration = 0;
        try {
            String[] split = durationString.split(":");
            if (split.length == 4) {
                String daysString = split[0].trim().replaceAll("(\\d+)d", "$1");
                int days = Integer.parseInt(daysString);
                duration += days * ONE_DAY_IN_MILLIS;

                int hours = Integer.parseInt(split[1].trim());
                duration += hours * ONE_HOUR_IN_MILLIS;

                int minutes = Integer.parseInt(split[2].trim());
                duration += minutes * ONE_MINUTE_IN_MILLIS;

                int seconds = Integer.parseInt(split[3].trim());
                duration += seconds * ONE_SECOND_IN_MILLIS;
            } else { //if (split.length == 3)
                int hours = Integer.parseInt(split[0].trim());
                duration += hours * ONE_HOUR_IN_MILLIS;

                int minutes = Integer.parseInt(split[1].trim());
                duration += minutes * ONE_MINUTE_IN_MILLIS;

                int seconds = Integer.parseInt(split[2].trim());
                duration += seconds * ONE_SECOND_IN_MILLIS;
            }
        } catch (Exception e) {
            logger.error("Could not transform duration string", e);
        }
        return duration;
    }

    public static File getResourceAsFile(Class<?> referenceClass, String resourcePath) {
        ClassLoader classLoader = referenceClass.getClassLoader();
        return new File(classLoader.getResource(resourcePath).getFile().replace("%20", " "));
    }

    public static void discardLoadedProfileIfPresent(ProfilesApi profilesApi) {
        try {
            Path tmpFile = Files.createTempFile("deleteme", ".epp");
            profilesApi.saveProfile(new ProfilePath().path(tmpFile.toString()));
            Files.delete(tmpFile);
        } catch (Exception ignored) {
        }
    }

    public static String getCompilerPreferenceValue(String compilerShortName) {
        String shortName = compilerShortName.toUpperCase();
        if (shortName.equals("MINGW64")) {
            return Constants.PREF_COMPILER_MINGW_VALUE;
        } else if (shortName.equals("MSSDK71")) {
            return Constants.PREF_COMPILER_MSSDK71_VALUE;
        } else if (shortName.startsWith("MSVC")) {
            return shortName + "(64bit)";
        }
        // fallback, this might cause an error when no compiler preference value exists by this name
        return compilerShortName;
    }

    /**
     * Tries to set an arbitrary compiler. Returns the compiler preference value (e.g. "MinGW64 (64bit)") if succesful
     * or null if no compiler could be set.
     *
     * @return compiler preference value or null (if unsuccessful)
     */
    public static String setArbitraryCompiler() {
        Preference preference = new Preference().preferenceName(Constants.PREF_COMPILER_KEY);
        PreferencesApi prefApi = new PreferencesApi();
        for (String compilerPrefValue : Constants.getKnownCompilerPreferenceValues()) {
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
        Preference preference = new Preference().preferenceName(Constants.PREF_COMPILER_KEY);
        PreferencesApi prefApi = new PreferencesApi();
        String compilerPreferenceValue = getCompilerPreferenceValue(shortName);
        preference.setPreferenceValue(compilerPreferenceValue);
        prefApi.setPreferences(Arrays.asList(preference));
    }

    /**
     * Sets the specified compiler. Alternatively, tries to set an arbitrary fallback compiler.
     * 
     * @throws RuntimeException if no compiler can be set.
     */
    public static void setCompilerWithFallback(String compilerShortName, PrintStream jenkinsConsole)
        throws RuntimeException {
        String arbitraryCompiler = "n.a.";
        if (compilerShortName != null) {
            try {
                Util.setCompiler(compilerShortName);
            } catch (ApiException e) {
                arbitraryCompiler = Util.setArbitraryCompiler();
                if (arbitraryCompiler != null) {
                    jenkinsConsole.println("The selected compiler '" + compilerShortName
                        + "' could not be set. Falling back to arbitrary compiler '" + arbitraryCompiler + "'.");
                }
            }
        } else {
            arbitraryCompiler = Util.setArbitraryCompiler();
        }
        if (arbitraryCompiler == null) {
            throw new RuntimeException(
                "No compiler could not be set. Please verify if a supported compiler is installed (see BTC EmbeddedPlatform Install Guide)");
        }
    }

    /**
     * Configures the Matlab connection according to the specified version and instancePolicy.
     * 
     * @param version a version string, like "2020b"
     * @param instancePolicy the instance policy string (AUTO / NEVER / ALWAYS)
     * @throws ApiException if the selected compiler is not available
     */
    public static void configureMatlabConnection(String version, String instancePolicy) throws ApiException {
        // version policy --> CUSTOM
        Preference prefVersionPolicy = new Preference()
            .preferenceName(Constants.PREF_MATLAB_VERSION_POLICY_KEY)
            .preferenceValue(Constants.PREF_MATLAB_VERSION_POLICY_CUSTOM);

        // custom version: MATLAB R20XXa/b (64-bit)
        String extractedVersion = version.replaceAll(".*(20\\d\\d[a|b]).*", "$1");
        String versionPrefValue = "MATLAB R" + extractedVersion + " (64-bit)";
        Preference prefMatlabVersion = new Preference()
            .preferenceName(Constants.PREF_MATLAB_VERSION_KEY)
            .preferenceValue(versionPrefValue);

        // instance policy (AUTO / NEVER / ALWAYS)
        Preference prefInstancePolicy = new Preference()
            .preferenceName(Constants.PREF_MATLAB_INSTANCE_POLICY_KEY)
            .preferenceValue(instancePolicy.toUpperCase());

        PreferencesApi prefApi = new PreferencesApi();
        prefApi.setPreferences(Arrays.asList(prefVersionPolicy, prefMatlabVersion, prefInstancePolicy));
    }

}
