package com.btc.ep.plugins.embeddedplatform.util;

import java.util.Date;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcBackToBackTestStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcVectorGenerationStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateCStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;

public class TestStuff {

    /*
     * Currently requires minor modification in BtcStepExecution class to accept the fake step context
     */
    private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance();

    public static void main(String[] args) throws Exception {

        System.out.println(new Date(1627693920000l));

        if (args.length < 99)
            return;

        BtcStartupStep start = new BtcStartupStep();
        start.setInstallPath("E:/Program Files/BTC/ep2.10p0");
        start.setPort(29268);
        start.setAdditionalJvmArgs("-Xmx1g");
        start.start(DUMMY_CONTEXT).start();

        //        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("E:/profile_210.epp");
        //        profileLoad.setUpdateRequired(false);
        //        profileLoad.start(DUMMY_CONTEXT).start();

        BtcProfileCreateCStep profileCreateC = new BtcProfileCreateCStep("E:/profile_210_ccode_profilecreation.epp",
            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\C-Code\\FromScratch\\CodeModel.xml");
        profileCreateC.start(DUMMY_CONTEXT).start();

        BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
        vectorGen.setPll("F");
        vectorGen.setAnalyzeSubscopesHierarchically(false);
        vectorGen.setGlobalTimeout(5);
        vectorGen.setEngine("ATG");
        vectorGen.start(DUMMY_CONTEXT).start();

        BtcBackToBackTestStep b2b = new BtcBackToBackTestStep();
        b2b.setReference("SIL");
        b2b.setComparison("SIL");
        b2b.start(DUMMY_CONTEXT).start();

        BtcWrapUpStep wrapUp = new BtcWrapUpStep();
        wrapUp.start(DUMMY_CONTEXT).start();
    }

}
