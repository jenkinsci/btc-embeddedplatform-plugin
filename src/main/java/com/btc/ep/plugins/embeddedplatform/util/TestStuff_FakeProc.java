package com.btc.ep.plugins.embeddedplatform.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import hudson.Proc;

public class TestStuff_FakeProc extends Proc {
	
	private Process process;
	
	public TestStuff_FakeProc(List<String> cmd) throws IOException {
		this.process = new ProcessBuilder(cmd).start(); 
	}

	@Override
	public void kill() throws IOException, InterruptedException {
		process.destroyForcibly();
	}
	
	@Override
	public int join() throws IOException, InterruptedException {
		return 0;
	}
	
	@Override
	public boolean isAlive() throws IOException, InterruptedException {
		return process.isAlive();
	}
	
	@Override
	public InputStream getStdout() {
		return process.getInputStream();
	}
	
	@Override
	public OutputStream getStdin() {
		return process.getOutputStream();
	}
	
	@Override
	public InputStream getStderr() {
		return process.getErrorStream();
	}

}
