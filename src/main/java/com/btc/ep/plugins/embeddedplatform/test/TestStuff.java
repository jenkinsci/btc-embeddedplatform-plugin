package com.btc.ep.plugins.embeddedplatform.test;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorExportStep;

public class TestStuff {

    /*
     * Currently requires minor modification in BtcStepExecution class to accept the fake step context
     */
	private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance("C:/workspace/");
    private static final String BASE_DIR = "C:/Users/nathand/Desktop/PowerWindow/PowerWindow_EmbeddedCoder";

    public static void main(String[] args) throws Exception {
        BtcStartupStep start = new BtcStartupStep();
        start.setInstallPath("C:/Program Files/BTC/ep22.1p0");
        start.setPort(29268);
        start.setAdditionalJvmArgs("-Xmx2g");
        start.start(DUMMY_CONTEXT).start();

        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("C:/workspace/profile.epp");
        profileLoad.setUpdateRequired(false);
        profileLoad.start(DUMMY_CONTEXT).start();
        
        
//        String slModelPath = BASE_DIR + "/EmbeddedCoder/Autosar/autosar_swc_expfcns.slx";
//        BtcProfileCreateECStep profileCreateEC = new BtcProfileCreateECStep(slModelPath);
//        profileCreateEC.setCreateWrapperModel(true);
//        profileCreateEC.start(DUMMY_CONTEXT).start();
        

//        BtcMigrationSourceStep ms = new BtcMigrationSourceStep("E:/profile_211_ccms.epp");
//        ms.setInstallPath("E:/Program Files/BTC/ep2.11p0");
//        ms.setCodeModelPath(BASE_DIR + "/C-Code/FromScratch/CodeModel.xml");
//        //        ms.setSlModelPath(BASE_DIR + "/EmbeddedCoder/FromScratch/powerwindow_ec.slx");
//        //        ms.setSlScriptPath(BASE_DIR + "/EmbeddedCoder/FromScratch/start.m");
//        ms.setPort(29268);
//        ms.setAdditionalJvmArgs("-Xmx1g");
//        ms.setUpdateRequired(false);
//        ms.setMatlabVersion("2019b");
//        ms.setPll("F");
//        ms.setEngine("ATG");
//        ms.setAnalyzeSubscopesHierarchically(false);
//        ms.start(DUMMY_CONTEXT).start();
//
//        BtcMigrationTargetStep mt = new BtcMigrationTargetStep("E:/profile_211_ccms.epp");
//        mt.setInstallPath("E:/Program Files/BTC/ep2.11p0");
//        mt.setPort(29268);
//        mt.setAdditionalJvmArgs("-Xmx1g");
//        mt.setMatlabVersion("2020b");
//        mt.start(DUMMY_CONTEXT).start();

        //        BtcProfileCreateCStep profileCreateC = new BtcProfileCreateCStep("E:/profile_210_ccode_profilecreation.epp",
        //            BASE_DIR + "/C-Code/FromScratch/CodeModel.xml");
        //        profileCreateC.start(DUMMY_CONTEXT).start();

        //        BtcProfileCreateSLStep profileCreateSL = new BtcProfileCreateSLStep("E:/profile_210_sl_profilecreation.epp",
        //            BASE_DIR + "/Simulink/FromScratch/powerwindow_sl_v01.mdl",
        //            BASE_DIR + "/Simulink/FromScratch/powerwindow_sl_v01.xml",
        //            "2019b");
        //        profileCreateSL.setSlScriptPath(BASE_DIR + "/Simulink/FromScratch/start.m");
        //        profileCreateSL.start(DUMMY_CONTEXT).start();

        		System.out.println("Started import");
                BtcVectorExportStep vectorImportStep = new BtcVectorExportStep(
                    "C:/workspace/tests", "Excel");
                vectorImportStep.start(DUMMY_CONTEXT).start();
                System.out.println("Finished import");
                
        //        BtcToleranceImportStep tolImportStep = new BtcToleranceImportStep("E:/tolerances.xml");
        //        tolImportStep.setUseCase("RBT");
        //        tolImportStep.start(DUMMY_CONTEXT).start();
//
//                System.out.println("Started RBT");
//                BtcRBTStep rbt = new BtcRBTStep();
//                rbt.setExecutionConfigString("SIL");
//                rbt.setTestCasesBlacklist("SV_ATG_3");
//                rbt.setCreateReport(true);
//                rbt.start(DUMMY_CONTEXT).start();
//                System.out.println("Finished RBT");

        //        BtcProfileCreateTLStep tlProfile = new BtcProfileCreateTLStep("targetlink-profile-210.epp",
        //            BASE_DIR + "/TargetLink/FromScratch/PowerWindowController/powerwindow_tl_v01.slx",
        //            "2019b");
        //        tlProfile.setTlScriptPath(
        //            BASE_DIR + "/TargetLink/FromScratch/PowerWindowController/start.m");
        //        tlProfile.setReuseExistingCode(true);
        //        tlProfile.start(DUMMY_CONTEXT).start();
//
//                BtcProfileCreateECStep ecProfile = new BtcProfileCreateECStep("embeddedcoder-profile-210.epp",
//                    BASE_DIR + "/powerwindow_ec.slx");
//                ecProfile.setSlScriptPath(
//                    BASE_DIR + "/start.m");
//                ecProfile.setMatlabVersion("2019b");
//                ecProfile.start(DUMMY_CONTEXT).start();

                /*BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
                //vectorGen.setPll("F");
                vectorGen.setAnalyzeSubscopesHierarchically(false);
                vectorGen.setGlobalTimeout(50);
                vectorGen.setEngine("ATG+CV");
                vectorGen.start(DUMMY_CONTEXT).start();
                
                BtcVectorExportStep vecEx = new BtcVectorExportStep();
                vecEx.setExportDir("C:/workspace/tests");
                vecEx.setVectorKind("TC");
                vecEx.setVectorFormat("CSV");
                vecEx.start(DUMMY_CONTEXT).start();*/
        
        		/*System.out.println("Importing");
        		BtcVectorImportStep vecImp = new BtcVectorImportStep("C:/workspace/tests");
        		vecImp.setVectorKind("CSV");
        		// //vecImp.setVectorFormat("Excel");
        		vecImp.start(DUMMY_CONTEXT).start();*/
        		
        		/*BtcToleranceExportStep tolEx = new BtcToleranceExportStep("C:/workspace/tolerances/tol1.xml");
        		tolEx.setUseCase("B2B");
        		tolEx.start(DUMMY_CONTEXT).start();*/
        		

                /*BtcB2BStep b2b = new BtcB2BStep();
                b2b.setReference("SL MIL");
                b2b.setComparison("SIL");
                b2b.start(DUMMY_CONTEXT).start();*/
                
                /*BtcAddDomainCheckGoals domainCheckGoals = new BtcAddDomainCheckGoals();
                //domainCheckGoals.setDcXmlPath("C:/workspace/domain_checks.xml");
                domainCheckGoals.setRaster("30");
                domainCheckGoals.setScopePath("*");
                domainCheckGoals.setActivateRangeViolationCheck(true);
                domainCheckGoals.start(DUMMY_CONTEXT).start();*/
        
        		// TEST workflow
//        		BtcProfileCreateECStep prof = new BtcProfileCreateECStep("C:/workspace/ECprof1.epp",
//        				BASE_DIR + "/powerwindow_ec.slx");
//        		prof.setSlScriptPath(BASE_DIR + "/start.m");
//        		prof.setExportPath("C:/workspace/reports/");
//        		prof.start(DUMMY_CONTEXT).start();
		        /*BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("C:/workspace/ECprof1.epp");
		        profileLoad.setUpdateRequired(false);
		        profileLoad.start(DUMMY_CONTEXT).start();*/
        		
	// here
        		/*System.out.println("Generating vectors");
		        BtcVectorGenerationStep vectorGen = new BtcVectorGenerationStep();
                vectorGen.start(DUMMY_CONTEXT).start();
        		
        		/*System.out.println("B2B");
        		BtcB2BStep b2b = new BtcB2BStep();
        		b2b.start(DUMMY_CONTEXT).start();*/
        		
        		/*System.out.println("Exporting vectors");
        		BtcVectorExportStep ve = new BtcVectorExportStep("C:/workspace/tests", "Excel");
        		ve.setVectorKind("SV");
        		ve.start(DUMMY_CONTEXT).start();*/
        
        		/*System.out.println("importing execution records:");
        		BtcExecutionRecordImportStep er = new BtcExecutionRecordImportStep("C:/workspace/ExecutionRecords", "JK SIL");
        		er.start(DUMMY_CONTEXT).start();*/
        
        		/*System.out.println("exporting input restrictions");
        		BtcInputRestrictionsExportStep ires = new BtcInputRestrictionsExportStep("C:/workspace/inputRestrictions/export.xml");
        		ires.start(DUMMY_CONTEXT).start();*/
        
		        /*System.out.println("exporting tolerances");
				BtcToleranceExportStep ires = new BtcToleranceExportStep("C:/workspace/profile.epp", "C:/workspace/tolerances/export.xml");
				ires.start(DUMMY_CONTEXT).start();*/
        		
                BtcWrapUpStep wrapUp = new BtcWrapUpStep();
                wrapUp.setCloseEp(false);
                wrapUp.start(DUMMY_CONTEXT).start();
    }

}
