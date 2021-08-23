package com.btc.ep.plugins.embeddedplatform.util;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcRBTStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateSLStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorImportStep;

public class TestStuff {

    /*
     * Currently requires minor modification in BtcStepExecution class to accept the fake step context
     */
    private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance();
    private static final String BASE_DIR = "E:/EP2/SupportPlugins/EPJenkinsAutomation/TestSuite/Architectures";

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
        //            baseDir + "/C-Code/FromScratch/CodeModel.xml");
        //        profileCreateC.start(DUMMY_CONTEXT).start();

        BtcProfileCreateSLStep profileCreateSL = new BtcProfileCreateSLStep("E:/profile_210_sl_profilecreation.epp",
            BASE_DIR + "/Simulink/FromScratch/powerwindow_sl_v01.mdl",
            BASE_DIR + "/Simulink/FromScratch/powerwindow_sl_v01.xml",
            "2019b");
        profileCreateSL.setSlScriptPath(BASE_DIR + "/Simulink/FromScratch/start.m");
        profileCreateSL.start(DUMMY_CONTEXT).start();

        BtcVectorImportStep vectorImportStep = new BtcVectorImportStep(
            BASE_DIR + "/Simulink/FromScratch/testcases");
        vectorImportStep.start(DUMMY_CONTEXT).start();

        //        BtcToleranceImportStep tolImportStep = new BtcToleranceImportStep("E:/tolerances.xml");
        //        tolImportStep.setUseCase("RBT");
        //        tolImportStep.start(DUMMY_CONTEXT).start();

        BtcRBTStep rbt = new BtcRBTStep();
        rbt.setExecutionConfigString("SL MIL");
        rbt.setTestCasesBlacklist("TC-REQ_PW_3");
        rbt.setCreateReport(true);
        rbt.start(DUMMY_CONTEXT).start();

        //        BtcProfileCreateTLStep tlProfile = new BtcProfileCreateTLStep("targetlink-profile-210.epp",
        //            baseDir + "/TargetLink/FromScratch/PowerWindowController/powerwindow_tl_v01.slx",
        //            "2019b");
        //        tlProfile.setTlScriptPath(
        //            baseDir + "/TargetLink/FromScratch/PowerWindowController/start.m");
        //        tlProfile.setReuseExistingCode(true);
        //        tlProfile.start(DUMMY_CONTEXT).start();

        //        BtcProfileCreateECStep ecProfile = new BtcProfileCreateECStep("embeddedcoder-profile-210.epp",
        //            baseDir + "/EmbeddedCoder/FromScratch/powerwindow_ec.slx",
        //            "2019b");
        //        ecProfile.setSlScriptPath(
        //            baseDir + "/EmbeddedCoder/FromScratch/start.m");
        //        ecProfile.start(DUMMY_CONTEXT).start();

        //        BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
        //        vectorGen.setPll("F");
        //        vectorGen.setAnalyzeSubscopesHierarchically(false);
        //        vectorGen.setGlobalTimeout(5);
        //        vectorGen.setEngine("ATG");
        //        vectorGen.start(DUMMY_CONTEXT).start();

        //        BtcB2BStep b2b = new BtcB2BStep();
        //        b2b.setReference("SL MIL");
        //        b2b.setComparison("SIL");
        //        b2b.start(DUMMY_CONTEXT).start();

        BtcWrapUpStep wrapUp = new BtcWrapUpStep();
        wrapUp.setCloseEp(false);
        wrapUp.start(DUMMY_CONTEXT).start();
    }

}
