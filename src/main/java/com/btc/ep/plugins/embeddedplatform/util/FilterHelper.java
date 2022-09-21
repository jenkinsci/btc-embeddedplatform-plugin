package com.btc.ep.plugins.embeddedplatform.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openapitools.client.ApiException;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.model.Requirement;
import org.openapitools.client.model.RequirementBasedTestCase;

public class FilterHelper {

	/**
	 * Filters the given testCases by applying the whitelist & blacklist and the
	 * relevant requirements
	 * 
	 * @param rbTestCasesByScope   unfiltered testcases
	 * @param filteredRequirements the requirements
	 * @return the test cases that match the filter.
	 */
	public static List<RequirementBasedTestCase> filterTestCases(List<RequirementBasedTestCase> testCases,
			List<Requirement> filteredRequirements, List<String> blacklistedTestCases,
			List<String> whitelistedTestCases) {
		Set<String> validTcNames = new HashSet<>();
		List<RequirementBasedTestCase> filteredTestCases = new ArrayList<>();
		if (filteredRequirements != null) {
			for (Requirement req : filteredRequirements) {
				List<RequirementBasedTestCase> testCasesLinkedToCurrentReq;
				try {
					testCasesLinkedToCurrentReq = new RequirementBasedTestCasesApi()
							.getTestCasesByRequirementId(req.getUid(), false);
					Set<String> tcNames = testCasesLinkedToCurrentReq.stream().map(tc -> tc.getName())
							.collect(Collectors.toSet());
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
			List<String> blacklistedRequirements, List<String> whitelistedRequirements) {
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
	
}
