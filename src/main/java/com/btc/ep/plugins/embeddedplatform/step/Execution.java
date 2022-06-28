package com.btc.ep.plugins.embeddedplatform.step;

import jenkins.security.MasterToSlaveCallable;

public class Execution extends MasterToSlaveCallable<Object, Exception> {

	private static final long serialVersionUID = 9219262644077384364L;
	private transient AbstractBtcStepExecution abse;

	public Execution(AbstractBtcStepExecution abse) {
		this.abse = abse;
	}
	
	@Override
	public Object call() throws Exception {
		abse.performAction();
		return null;
	}

}
