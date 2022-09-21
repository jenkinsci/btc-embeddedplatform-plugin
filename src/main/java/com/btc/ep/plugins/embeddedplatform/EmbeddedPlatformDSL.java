package com.btc.ep.plugins.embeddedplatform;

import java.io.IOException;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;

import com.btc.ep.plugins.pipelinedsl.PipelineDSLGlobal;

import hudson.Extension;

@Extension
public class EmbeddedPlatformDSL extends PipelineDSLGlobal {

	@Override
	public String getFunctionName() {
		return "btc";
	}

	@Extension
	public static class MiscWhitelist extends ProxyWhitelist {

		public MiscWhitelist() throws IOException {
			super(new StaticWhitelist("method java.util.Map$Entry getKey", "method java.util.Map$Entry getValue"));
		}
	}

}
