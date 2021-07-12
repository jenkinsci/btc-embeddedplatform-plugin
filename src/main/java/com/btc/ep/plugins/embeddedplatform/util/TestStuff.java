package com.btc.ep.plugins.embeddedplatform.util;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcBackToBackTestStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcVectorGenerationStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;

public class TestStuff {

    /*
     * Currently requires minor modification in BtcStepExecution class to accept the fake step context
     */
    private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance();

    public static void main(String[] args) throws Exception {

        BtcStartupStep start = new BtcStartupStep();
        start.setInstallPath("E:/Program Files/BTC/ep2.9p1");
        start.setAdditionalJvmArgs("-Xmx1g");
        start.start(DUMMY_CONTEXT).start();

        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("E:/profile_29.epp");
        profileLoad.setUpdateRequired(false);
        profileLoad.start(DUMMY_CONTEXT).start();

        BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
        vectorGen.start(DUMMY_CONTEXT).start();

        BtcBackToBackTestStep b2b = new BtcBackToBackTestStep();
        b2b.setReference("SIL");
        b2b.setComparison("SIL");
        b2b.start(DUMMY_CONTEXT).start();

        BtcWrapUpStep wrapUp = new BtcWrapUpStep();
        wrapUp.start(DUMMY_CONTEXT).start();
    }

}
