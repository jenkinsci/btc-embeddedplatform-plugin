package com.btc.ep.plugins.embeddedplatform.util;

import java.io.File;
import java.util.Date;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.btc.ep.plugins.embeddedplatform.reporting.ReportService;
import com.btc.ep.plugins.embeddedplatform.reporting.project.JenkinsAutomationReport;
import com.btc.ep.plugins.embeddedplatform.reporting.project.PilInfoSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.StepArgSection;
import com.btc.ep.plugins.embeddedplatform.reporting.project.TestStepSection;
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
        start.start(DUMMY_CONTEXT).start();

        BtcProfileLoadStep profileLoad = new BtcProfileLoadStep("E:/profile_29.epp");
        profileLoad.setUpdateRequired(false);
        profileLoad.start(DUMMY_CONTEXT).start();

        /*
         * REPORTING ----------------------------------------------------------
         */
        ReportService reportService = ReportService.getInstance();

        // Reporting
        JenkinsAutomationReport reportData = new JenkinsAutomationReport();
        TestStepSection testStepSection = new TestStepSection();
        PilInfoSection pilInfoSection = new PilInfoSection();
        StepArgSection testStepArgumentSection = new StepArgSection();
        Date startDate = new Date();
        String startDateString = Util.DATE_FORMAT.format(startDate);
        reportData.setStartDate(startDateString);
        //        MetaInfoSection metaInfoSection = new MetaInfoSection();
        //        Map<String, String> metadata = new ProfilesApi().getCurrentProfile().getMetadata();
        //        metaInfoSection.setProfileData(metadata);
        //        reportData.addSection(metaInfoSection);

        reportData.addSection(testStepSection);
        testStepArgumentSection.setSteps(testStepSection.getSteps());
        reportData.addSection(testStepArgumentSection);
        // checkForPILResults();
        reportData.addSection(pilInfoSection);
        String endDate = Util.DATE_FORMAT.format(new Date());
        reportData.setEndDate(endDate);
        String durationString = Util.getTimeDiffAsString(new Date(), startDate);
        reportData.setDuration(durationString);
        File report = reportService.generateProjectReport(reportData);

        if (args.length < 222) {
            return;
        }
        /*
         * END REPORTING -------------------------------------------------------
         */

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
