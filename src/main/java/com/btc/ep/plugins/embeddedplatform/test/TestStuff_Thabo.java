package com.btc.ep.plugins.embeddedplatform.test;

import java.util.Date;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcRBTStep;
import com.btc.ep.plugins.embeddedplatform.step.analysis.BtcVectorGenerationStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateCStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileCreateECStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcProfileLoadStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.step.basic.BtcWrapUpStep;
import com.btc.ep.plugins.embeddedplatform.step.io.BtcVectorImportStep;

public class TestStuff_Thabo {

	private static final String WS = "E:/workspace/";
    private static final String BASE_DIR = "E:/EP2/SupportPlugins/EPJenkinsAutomation/TestSuite/Architectures";
    private static final String EC_EPP_FILE = "autosar_swc_expfcns.epp";

    // Currently requires minor modification in BtcStepExecution class to accept the fake step context
    private static final StepContext DUMMY_CONTEXT = StepContextStub.getInstance(WS);

    public static void main(String[] args) throws Exception {
        
    	startEP();
    	
    	createCProfile(BASE_DIR + "/C-Code/FromScratch/CodeModel.xml");
    	
    	Date d1 = new Date();
    	
//    	createCProfile(BASE_DIR + "/C-Code/FromScratch/CodeModel.xml");
    	
    	Date d2 = new Date();
    	
//    	createCProfile(BASE_DIR + "/C-Code/FromScratch/CodeModel_reduced.xml");
    	
    	Date d3 = new Date();
    	
//    	createCProfile(BASE_DIR + "/C-Code/FromScratch/CodeModel_lib.xml");
    	
    	Date d4 = new Date();
    	
    	System.out.println("CodeModel: " + (d2.getTime() - d1.getTime()) + "ms");
    	System.out.println("CodeModel: " + (d3.getTime() - d2.getTime()) + "ms");
    	System.out.println("CodeModel: " + (d4.getTime() - d3.getTime()) + "ms");
    	
//        loadEcProfile(WS + "/" + EC_EPP_FILE);
//        importTestCases(BASE_DIR + "/EmbeddedCoder/Autosar/testcases");
//        runRBT("SIL");
        wrapUp();
        
    }


	private static void createCProfile(String codeModelPath) throws Exception {
		BtcProfileCreateCStep step = new BtcProfileCreateCStep(codeModelPath);
		step.start(DUMMY_CONTEXT).start();
	}


	protected static void runRBT(String ecs) throws Exception {
		// rbt
        BtcRBTStep rbtExecution = new BtcRBTStep();
        rbtExecution.setCreateReport(true);
        rbtExecution.setExecutionConfigString(ecs);
        rbtExecution.start(DUMMY_CONTEXT).start();
	}


	protected static void generateVectors(String pll) throws Exception {
		// vector generation
        BtcVectorGenerationStep generateVectors = new BtcVectorGenerationStep();
        generateVectors.setPll(pll);
        generateVectors.setCreateReport(true);
        generateVectors.start(DUMMY_CONTEXT).start();
	}

    
    /*
     * To have a cleaner overview of the workflows, each step gets 1 dedicated method
     */

    protected static void importTestCases(String importDir) throws Exception {
    	// tc import
    	BtcVectorImportStep vectorImport = new BtcVectorImportStep(importDir);
    	vectorImport.start(DUMMY_CONTEXT).start();
    }
    
	protected static void startEP() throws Exception {
		// startup
    	BtcStartupStep start = new BtcStartupStep();
        start.setInstallPath("E:/Program Files/BTC/ep22.1p0");
        start.setAdditionalJvmArgs("-Xmx2g");
        start.start(DUMMY_CONTEXT).start();
	}

	protected static void loadEcProfile(String profilePath) throws Exception {
		// load epp
        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep(profilePath);
        profileLoad.start(DUMMY_CONTEXT).start();
	}

	protected static void wrapUp() throws Exception {
		// save profile + reports
        BtcWrapUpStep wrapUp = new BtcWrapUpStep();
        wrapUp.setCloseEp(false);
        wrapUp.start(DUMMY_CONTEXT).start();
	}

	//createEcProfile(BASE_DIR + "/EmbeddedCoder/Autosar/autosar_swc_expfcns.slx", null);
	static void createEcProfile(String slModelPath, String slScriptPath) throws Exception {
		// ec-import
        BtcProfileCreateECStep profileCreateEC = new BtcProfileCreateECStep(slModelPath);
        if (slScriptPath != null) {
        	profileCreateEC.setSlScriptPath(slScriptPath);
        }
        profileCreateEC.setCreateWrapperModel(true);
        profileCreateEC.start(DUMMY_CONTEXT).start();
	}

}
