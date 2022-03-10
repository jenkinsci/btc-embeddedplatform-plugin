package com.btc.ep.plugins.embeddedplatform.reporting.project;

/**
 * This class ....
 * 
 * @author me
 */
public class TestStep extends BasicStep {

	public TestStep(String name) {
		super(name);
	}

	public void addArgument(String key, Object value) {
		String stringValue = String.valueOf(value).equals("null") ? "n.a." : String.valueOf(value);
		this.args.put(key, stringValue);
	}

	public void addDetail(String detail) {
		this.details.add(detail);
	}

	/**
	 * Adds the link to the sub-report
	 * 
	 * @param detailsLink expects relative path from the reportDir
	 */
	public void addDetailsLink(String detailsLink) {
		this.detailsLinks.add(detailsLink);

	}

}
