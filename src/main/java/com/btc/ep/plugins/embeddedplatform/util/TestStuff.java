package com.btc.ep.plugins.embeddedplatform.util;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcRBTStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateSLStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcToleranceImportStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorImportStep;

public class TestStuff {

    /*
     * Currently requires minor modification in BtcStepExecution class to accept the fake step context
     */
    private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance();

    public static void main(String[] args) throws Exception {

        BtcStartupStep start = new BtcStartupStep();
        start.setInstallPath("E:/Program Files/BTC/ep2.10p0");
        start.setPort(29268);
        start.setAdditionalJvmArgs("-Xmx1g");
        start.start(DUMMY_CONTEXT).start();

        //        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("E:/profile_210.epp");
        //        profileLoad.setUpdateRequired(false);
        //        profileLoad.start(DUMMY_CONTEXT).start();

        //        BtcProfileCreateCStep profileCreateC = new BtcProfileCreateCStep("E:/profile_210_ccode_profilecreation.epp",
        //            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\C-Code\\FromScratch\\CodeModel.xml");
        //        profileCreateC.start(DUMMY_CONTEXT).start();

        BtcProfileCreateSLStep profileCreateSL = new BtcProfileCreateSLStep("E:\\profile_210_sl_profilecreation.epp",
            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\Simulink\\FromScratch\\powerwindow_sl_v01.mdl",
            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\Simulink\\FromScratch\\powerwindow_sl_v01.xml",
            "2019b");
        profileCreateSL.setSlScriptPath(
            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\Simulink\\FromScratch\\start.m");
        profileCreateSL.start(DUMMY_CONTEXT).start();

        BtcVectorImportStep vectorImportStep = new BtcVectorImportStep(
            "E:\\EP2\\SupportPlugins\\EPJenkinsAutomation\\TestSuite\\Architectures\\Simulink\\FromScratch\\testcases");
        vectorImportStep.start(DUMMY_CONTEXT).start();

        BtcToleranceImportStep tolImportStep = new BtcToleranceImportStep("E:\\tolerances.xml");
        tolImportStep.setUseCase("RBT");
        tolImportStep.start(DUMMY_CONTEXT).start();

        BtcRBTStep rbtStep = new BtcRBTStep();
        rbtStep.setExecutionConfigString("SL MIL");
        rbtStep.setRequirementsBlacklist("KEKS");
        rbtStep.setCreateReport(true);
        rbtStep.start(DUMMY_CONTEXT).start();

        //        BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
        //        vectorGen.setPll("F");
        //        vectorGen.setAnalyzeSubscopesHierarchically(false);
        //        vectorGen.setGlobalTimeout(5);
        //        vectorGen.setEngine("ATG");
        //        vectorGen.start(DUMMY_CONTEXT).start();
        //
        //        BtcBackToBackTestStep b2b = new BtcBackToBackTestStep();
        //        b2b.setReference("SIL");
        //        b2b.setComparison("SIL");
        //        b2b.start(DUMMY_CONTEXT).start();

        BtcWrapUpStep wrapUp = new BtcWrapUpStep();
        wrapUp.setCloseEp(false);
        wrapUp.start(DUMMY_CONTEXT).start();
    }

}
