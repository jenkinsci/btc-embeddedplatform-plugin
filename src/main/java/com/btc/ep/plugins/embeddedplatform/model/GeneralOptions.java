package com.btc.ep.plugins.embeddedplatform.model;

public class GeneralOptions {

	private Integer port;
	private String installPath;
	private Integer timeout; // startup timeout in seconds
	private String additionalJvmArgs;
	private String licensingPackage;
	private String licenseLocation;
	private Boolean simplyConnect;
	
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public String getInstallPath() {
		return installPath;
	}
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}
	public Integer getTimeout() {
		return timeout;
	}
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
	public String getAdditionalJvmArgs() {
		return additionalJvmArgs;
	}
	public void setAdditionalJvmArgs(String additionalJvmArgs) {
		this.additionalJvmArgs = additionalJvmArgs;
	}
	public String getLicensingPackage() {
		return licensingPackage;
	}
	public void setLicensingPackage(String licensingPackage) {
		this.licensingPackage = licensingPackage;
	}
	public String getLicenseLocation() {
		return licenseLocation;
	}
	public void setLicenseLocation(String licenseLocation) {
		this.licenseLocation = licenseLocation;
	}
	public Boolean getSimplyConnect() {
		return simplyConnect;
	}
	public void setSimplyConnect(Boolean simplyConnect) {
		this.simplyConnect = simplyConnect;
	}
	
}
