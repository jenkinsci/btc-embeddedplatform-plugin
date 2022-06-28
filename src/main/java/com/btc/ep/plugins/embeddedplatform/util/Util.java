package com.btc.ep.plugins.embeddedplatform.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.PreferencesApi;
import org.openapitools.client.api.RequirementsApi;
import org.openapitools.client.api.ScopesApi;
import org.openapitools.client.model.Preference;
import org.openapitools.client.model.Requirement;
import org.openapitools.client.model.RequirementSource;
import org.openapitools.client.model.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

public class Util {

	private static Logger logger = LoggerFactory.getLogger(Util.class);

	private static final int ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
	private static final int ONE_HOUR_IN_MILLIS = 1000 * 60 * 60;
	private static final int ONE_MINUTE_IN_MILLIS = 1000 * 60;
	private static final int ONE_SECOND_IN_MILLIS = 1000;

	public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM,
			Locale.getDefault());

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
	 * Returns true if none of the given objects are null. False if a null object is found.
	 * @param objects
	 * @return
	 */
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
		int days = (int) duration / (ONE_DAY_IN_MILLIS);
		int hours = (int) (duration / (ONE_HOUR_IN_MILLIS)) % 24;
		int minutes = (int) (duration / (ONE_MINUTE_IN_MILLIS)) % 60;
		int seconds = (int) (duration / (ONE_SECOND_IN_MILLIS)) % 60;
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
			} else { // if (split.length == 3)
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
		File file = null;
		ClassLoader classLoader = referenceClass.getClassLoader();
		URL res = classLoader.getResource(resourcePath);
		if (res.toString().startsWith("jar:")) {
			try {
				String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1,
						resourcePath.lastIndexOf("."));
				String extension = resourcePath.substring(resourcePath.lastIndexOf("."));
				InputStream input = classLoader.getResourceAsStream(resourcePath);
				file = File.createTempFile(fileName, extension);
				OutputStream out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];

				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.flush();
				out.close();
				input.close();
				Path renamed = Files.move(file.toPath(), Paths.get(file.getParent(), fileName + extension),
						StandardCopyOption.REPLACE_EXISTING);
				file = renamed.toFile();
				file.deleteOnExit();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} else {
			// this will probably work in your IDE, but not from a JAR
			file = new File(res.getFile());
		}

		return file;
	}

	

	/**
	 * Configures the Matlab connection according to the specified version and
	 * instancePolicy. Does nothing if the specified Matlab version is null
	 * 
	 * @param version        a version string, like "2020b"
	 * @param instancePolicy the instance policy string (AUTO / NEVER / ALWAYS)
	 * @throws ApiException if the selected compiler is not available
	 */
	public static void configureMatlabConnection(String version, String instancePolicy) throws ApiException {
		if (version != null) {
			// version policy --> CUSTOM
			Preference prefVersionPolicy = new Preference().preferenceName(Constants.PREF_MATLAB_VERSION_POLICY_KEY)
					.preferenceValue(Constants.PREF_MATLAB_VERSION_POLICY_CUSTOM);
			// custom version: MATLAB R20XXa/b (64-bit)
			String extractedVersion = version.replaceAll(".*(20\\d\\d[a|b]).*", "$1");
			String versionPrefValue = "MATLAB R" + extractedVersion + " (64-bit)";
			Preference prefMatlabVersion = new Preference().preferenceName(Constants.PREF_MATLAB_VERSION_KEY)
					.preferenceValue(versionPrefValue);
			// instance policy (AUTO / NEVER / ALWAYS)
			Preference prefInstancePolicy = new Preference().preferenceName(Constants.PREF_MATLAB_INSTANCE_POLICY_KEY)
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
	 * Returns the toplevel scope.
	 *
	 * @return the toplevel scope
	 * @throws ApiException if the scope cannot be determined
	 */
	public static Scope getToplevelScope() throws ApiException {
		try {
			return new ScopesApi().getScopesByQuery1(null, AbstractBtcStepExecution.TRUE).get(0);
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
	 * Applies the non-null-values of matching fields from the sourceStep to the
	 * targetStep.<br>
	 * Example:<br>
	 * - Source Step (name: foo, age: 30, street: null), TargetStep (name,
	 * phoneNumber, street);<br>
	 * - applies value of 'name' from source to target<br>
	 * <br>
	 * 
	 * Fields are matched based on name and type.<br>
	 * <br>
	 * 
	 * Returns the targetStep.<br>
	 * <br>
	 * 
	 * <b>IMPORTANT</b>: Source step should not use primitive types to prevent
	 * unless it defines a meaningful default value.
	 *
	 * @param source     source object, may be null (then this is a noop)
	 * @param targetStep
	 */
	@SuppressWarnings("unchecked")
	public static Step applyMatchingFields(Object source, Step targetStep) {
		if (source != null) {
			if (source instanceof Map) {
				// approach using the source object's map entries
				for (Entry<String, Object> entry : ((Map<String, Object>) source).entrySet()) {
					for (Field targetField : targetStep.getClass().getDeclaredFields()) {
						// matching can only happen based on names
						if (entry.getKey().equals(targetField.getName())) {
							try {
								String capitalizedFieldName = StringUtils.capitalize(targetField.getName());
								String setterName = "set" + capitalizedFieldName;
								Method setter = targetStep.getClass().getMethod(setterName, targetField.getType());
								if (entry.getValue() != null) {
									setter.invoke(targetStep, entry.getValue());
								}
							} catch (Exception ignored) {
								ignored.printStackTrace();
							}
						}
					}
				}
			} else {
				// approach using the source object's getters
				for (Field sourceField : source.getClass().getDeclaredFields()) {
					for (Field targetField : targetStep.getClass().getDeclaredFields()) {
						// if name and type of the fields match:
						if (sourceField.getName().equals(targetField.getName())
								&& typesMatch(sourceField.getType(), targetField.getType())) {
							try {
								String capitalizedFieldName = StringUtils.capitalize(targetField.getName());
								String setterName = "set" + capitalizedFieldName;
								String getterPart = (targetField.getType().getName().equalsIgnoreCase("boolean") ? "is"
										: "get");
								String getterName = capitalizedFieldName.startsWith("Is") ? targetField.getName()
										: getterPart + capitalizedFieldName;
								Method setter = targetStep.getClass().getMethod(setterName, targetField.getType());
								Method getter = source.getClass().getMethod(getterName);
								Object value = getter.invoke(source);
								if (value != null) {
									setter.invoke(targetStep, value);
								}
							} catch (Exception ignored) {
								ignored.printStackTrace();
							}
						}
					}
				}
			}

		}
		return targetStep;
	}

	/**
	 * Checks if two types match. This method does not strictly compare
	 * object-equality but also considers<br>
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
			if ((type1.getName().equals(entry.getKey()) && type2.getName().equals(entry.getValue()))
					|| (type2.getName().equals(entry.getKey()) && type1.getName().equals(entry.getValue()))) {
				return true;
			}
		}
		return false;
	}
	
	public static void transfer(InputStream source, OutputStream target, boolean closeStreams) throws IOException {
	    try {
			byte[] buf = new byte[8192];
			int length;
			while ((length = source.read(buf)) > 0) {
			    target.write(buf, 0, length);
			}
		} finally {
			if (closeStreams) {
				try {
					if (source != null) source.close();
					if (target != null) target.close();
				} catch(Exception ignored) {
				}
			}			
		}
	}

	/**
	 * Returns the content of the file as a String expecting utf8 encoded files.
	 *
	 * @param path path to the file
	 * @return the content of the file as a String or an empty String if the file is
	 *         not available.
	 * @throws UnsupportedEncodingException 
	 */
	public static String readStringFromStream(InputStream inputStream) throws UnsupportedEncodingException {
		String content = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8.toString())).lines().collect(Collectors.joining("\n"));
		return content;
	}

	/**
	 * Extracts parts from a space separated string while ignoring quoted spaces.
	 * This input:  "C:/Program Files/BTC something else 'another thing'
	 *     yields: [ "C:/Program Files/BTC", "something", "else", "another thing" ]
	 * @param startupScriptPath
	 * @return the individual parts
	 */
	public static List<String> extractSpaceSeparatedParts(String quoteSensitiveSpaceSeparatedString) {
		List<String> matchList2 = new ArrayList<String>();
		Pattern regex2 = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher2 = regex2.matcher(quoteSensitiveSpaceSeparatedString);
		while (regexMatcher2.find()) {
		    if (regexMatcher2.group(1) != null) {
		        // Add double-quoted string without the quotes
		        matchList2.add(regexMatcher2.group(1));
		    } else if (regexMatcher2.group(2) != null) {
		        // Add single-quoted string without the quotes
		        matchList2.add(regexMatcher2.group(2));
		    } else {
		        // Add unquoted word
		        matchList2.add(regexMatcher2.group());
		    }
		}
		return matchList2;
	}

}
