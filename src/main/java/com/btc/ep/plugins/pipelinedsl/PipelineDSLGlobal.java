/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.btc.ep.plugins.pipelinedsl;

import java.io.InputStreamReader;
import java.io.Reader;

import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.CpsThread;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;

public abstract class PipelineDSLGlobal extends GlobalVariable {

	public abstract String getFunctionName();

	@Override
	public String getName() {
		return getFunctionName();
	}

	@Override
	public Object getValue(CpsScript script) throws Exception {
		Binding binding = script.getBinding();

		CpsThread c = CpsThread.current();
		if (c == null)
			throw new IllegalStateException("Expected to be called from CpsThread");

		ClassLoader cl = getClass().getClassLoader();

		String scriptPath = "dsl/" + getFunctionName() + ".groovy";
		Reader r = new InputStreamReader(cl.getResourceAsStream(scriptPath), "UTF-8");

		GroovyCodeSource gsc = new GroovyCodeSource(r, getFunctionName() + ".groovy",
				cl.getResource(scriptPath).getFile());
		gsc.setCachable(true);

		@SuppressWarnings("unchecked")
		Object pipelineDSL = c.getExecution().getShell().getClassLoader().parseClass(gsc).getDeclaredConstructor()
				.newInstance();
		binding.setVariable(getName(), pipelineDSL);
		r.close();

		return pipelineDSL;
	}

}
