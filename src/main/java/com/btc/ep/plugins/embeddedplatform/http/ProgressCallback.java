package com.btc.ep.plugins.embeddedplatform.http;

public interface ProgressCallback {

	void updateProgressValue(int value);

	void updateProgressMessage(String message);

}
