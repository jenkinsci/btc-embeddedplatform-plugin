package com.btc.ep.plugins.embeddedplatform.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.PreferencesApi;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.RequirementsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Preference;
import org.openapitools.client.model.Requirement;
import org.openapitools.client.model.RequirementBasedTestCase;
import org.openapitools.client.model.RequirementSource;
import org.openapitools.client.model.Scope;
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

    public static boolean notNull(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                return false;
            }
        }
        return false;
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
     * Does nothing if the specified Matlab version is null
     * 
     * @param version a version string, like "2020b"
     * @param instancePolicy the instance policy string (AUTO / NEVER / ALWAYS)
     * @throws ApiException if the selected compiler is not available
     */
    public static void configureMatlabConnection(String version, String instancePolicy) throws ApiException {
        if (version != null) {
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

    /**
     * Returns all requirements from all req sources.
     *
     * @return A list of requirements (can be empty).
     */
    public static List<Requirement> getAllRequirements() {
        List<Requirement> requirements = new ArrayList<>();
        try {
            RequirementsApi reqApi = new RequirementsApi();
            List<RequirementSource> allRequirementSources = reqApi.getAllRequirementSources();
            for (RequirementSource requirementSource : allRequirementSources) {
                requirements.addAll(reqApi.getAllRequirementsOfRequirementSource(requirementSource.getUid()));
            }
        } catch (ApiException ignored) {
            // can be 404 if there's no requirements
        }
        return requirements;
    }

    /**
     * Utility function to get the values from a String with a comma separated list
     * of values. Will not return null, but can return an empty list.
     */
    public static List<String> getValuesFromCsv(String csvString) {
        List<String> values = new ArrayList<>();
        try {
            String[] rawValues = csvString.split(",");
            for (String rawValue : rawValues) {
                if (!rawValue.trim().isEmpty()) {
                    values.add(rawValue.trim());
                }
            }
        } catch (Exception e) {
            // NPE, etc. due to null input
        }
        return values;
    }

    /**
     * Filters the given testCases by applying the whitelist & blacklist and the relevant requirements
     * 
     * @param rbTestCasesByScope unfiltered testcases
     * @param filteredRequirements the requirements
     * @return the test cases that match the filter.
     */
    public static List<RequirementBasedTestCase> filterTestCases(List<RequirementBasedTestCase> testCases,
        List<Requirement> filteredRequirements, List<String> blacklistedTestCases, List<String> whitelistedTestCases) {
        Set<String> validTcNames = new HashSet<>();
        List<RequirementBasedTestCase> filteredTestCases = new ArrayList<>();
        if (filteredRequirements != null) {
            for (Requirement req : filteredRequirements) {
                List<RequirementBasedTestCase> testCasesLinkedToCurrentReq;
                try {
                    testCasesLinkedToCurrentReq =
                        new RequirementBasedTestCasesApi().getTestCasesByRequirementId(req.getUid(), false);
                    Set<String> tcNames =
                        testCasesLinkedToCurrentReq.stream().map(tc -> tc.getName()).collect(Collectors.toSet());
                    validTcNames.addAll(tcNames);
                } catch (ApiException ignored) {
                }

            }
        }
        for (RequirementBasedTestCase tc : testCases) {
            if (blacklistedTestCases.contains(tc.getName())) {
                System.out.println("Removing test case " + tc.getName() + " because it's blacklisted.");
                continue;
            }
            if (!whitelistedTestCases.isEmpty() && !whitelistedTestCases.contains(tc.getName())) {
                System.out.println("Removing test case " + tc.getName() + " because it's not whitelisted.");
                continue;
            }
            if (filteredRequirements != null && !validTcNames.contains(tc.getName())) {
                System.out.println("Removing test case " + tc.getName()
                    + " because it's not linked to any of the relevant requirements.");
                continue;
            }
            filteredTestCases.add(tc);
        }
        return filteredTestCases;
    }

    /**
     * Filters the given requirements by applying the whitelist & blacklist.
     *
     * @param requirements unfiltered requirements
     * @return the requirements that match the filter.
     */
    public static List<Requirement> filterRequirements(List<Requirement> requirements,
        List<String> blacklistedRequirements,
        List<String> whitelistedRequirements) {
        List<Requirement> filteredRequirements = new ArrayList<>();
        for (Requirement req : requirements) {
            if (blacklistedRequirements.contains(req.getName())) {
                continue;
            }
            if (whitelistedRequirements.isEmpty() || whitelistedRequirements.contains(req.getName())) {
                filteredRequirements.add(req);
            }
        }
        return filteredRequirements;
    }

    /**
     * Returns true, if the scopeName matches the filter.
     *
     * @param scopeName the scope name
     * @return true, if the scopeName matches the filter
     */
    public static boolean matchesScopeFilter(String scopeName, List<String> blacklistedScopes,
        List<String> whitelistedScopes) {
        if (blacklistedScopes.contains(scopeName)) {
            return false;
        }
        return whitelistedScopes.isEmpty() || whitelistedScopes.contains(scopeName);
    }

    /**
     * Returns the toplevel scope.
     *
     * @return the toplevel scope
     * @throws ApiException if the scope cannot be determined
     */
    public static Scope getToplevelScope() throws ApiException {
        try {
            return new ScopesApi().getScopesByQuery1(null, true).get(0);
        } catch (Exception e) {
            throw new ApiException("Could not determine the toplevel scope");
        }
    }

    /**
     * Returns a list of Requirements Sources (may be empty).
     *
     * @return a list of Requirements Sources (may be empty)
     */
    public static List<RequirementSource> getRequirementSources() {
        try {
            return new RequirementsApi().getAllRequirementSources();
        } catch (ApiException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Applies the non-null-values of matching fields from the sourceStep to the targetStep.<br>
     * Example:<br>
     * - Source Step (name: foo, age: 30, street: null), TargetStep (name, phoneNumber, street);<br>
     * - applies value of 'name' from source to target<br><br>
     * 
     * Fields are matched based on name and type.<br><br>
     * 
     * Returns the targetStep.<br><br>
     * 
     * <b>IMPORTANT</b>: Source step should not use primitive types to prevent unless it defines a meaningful default
     * value.
     *
     * @param sourceStep
     * @param targetStep
     */
    public static Step applyMatchingFields(Step sourceStep, Step targetStep) {
        for (Field sourceField : sourceStep.getClass().getDeclaredFields()) {
            for (Field targetField : targetStep.getClass().getDeclaredFields()) {
                // if name and type of the fields match:
                if (sourceField.getName().equals(targetField.getName())
                    && typesMatch(sourceField.getType(), targetField.getType())) {
                    try {
                        String capitalizedFieldName = StringUtils.capitalize(targetField.getName());
                        String setterName = "set" + capitalizedFieldName;
                        String getterPart =
                            (targetField.getType().getName().equalsIgnoreCase("boolean") ? "is" : "get");
                        String getterName = capitalizedFieldName.startsWith("Is") ? targetField.getName()
                            : getterPart + capitalizedFieldName;
                        Method setter = targetStep.getClass().getMethod(setterName, targetField.getType());
                        Method getter = sourceStep.getClass().getMethod(getterName);
                        Object value = getter.invoke(sourceStep);
                        if (value != null) {
                            setter.invoke(targetStep, value);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return targetStep;
    }

    /**
     * Checks if two types match. This method does not strictly compare object-equality but also considers<br>
     * <ul>
     * <li>java.lang.Boolean to match boolean</li>
     * <li>java.lang.Integer to match int</li>
     * <li>java.lang.Float to match float</li>
     * <li>java.lang.Double to match double</li>
     * </ul>
     * 
     * @return true if they match, false otherwise
     */
    private static boolean typesMatch(Class<?> type1, Class<?> type2) {
        if (type1.equals(type2)) {
            return true;
        }
        Map<String, String> matchingTypes = new HashMap<>();
        matchingTypes.put("java.lang.Boolean", "boolean");
        matchingTypes.put("java.lang.Integer", "int");
        matchingTypes.put("java.lang.Float", "float");
        matchingTypes.put("java.lang.Double", "double");
        for (Entry<String, String> entry : matchingTypes.entrySet()) {
            if ((type1.getName().equals(entry.getKey()) && type2.getName().equals(entry.getValue())) ||
                (type2.getName().equals(entry.getKey()) && type1.getName().equals(entry.getValue()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the content of the file as a String.
     *
     * @param path
     *            path to the file
     * @return the content of the file as a String or an empty String if the file is
     *         not available.
     */
    public static String readStringFromFile(String path) {
        StringBuilder sb = new StringBuilder();
        // Read file line by line using uft-8 encoding
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);) {
            while (reader.ready()) {
                sb.append(reader.readLine()).append("\n");
            }
            isr.close();
            reader.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return sb.toString();
    }

    /**
     * Writes the given String to the specified file. Existing files will be
     * overwritten.
     *
     * @param path
     *            the path to the file
     * @param content
     *            the content to write
     */
    public static void writeStringToFile(String path, String content) {
        // write file using utf-8 encoding
        try (OutputStreamWriter osr = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(osr)) {
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

}
