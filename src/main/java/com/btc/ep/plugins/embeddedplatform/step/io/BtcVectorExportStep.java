package com.btc.ep.plugins.embeddedplatform.step.io;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ArchitecturesApi;
import org.openapitools.client.api.ProfilesApi;
import org.openapitools.client.api.RequirementBasedTestCasesApi;
import org.openapitools.client.api.StimuliVectorsApi;
import org.openapitools.client.model.Architecture;
import org.openapitools.client.model.B2BStimuliVector;
import org.openapitools.client.model.Job;
import org.openapitools.client.model.Profile;
import org.openapitools.client.model.RestStimuliVectorExportInfo;
import org.openapitools.client.model.RestVectorExportDetails;

import com.btc.ep.plugins.embeddedplatform.http.HttpRequester;
import com.btc.ep.plugins.embeddedplatform.step.AbstractBtcStepExecution;

import hudson.Extension;
import hudson.model.TaskListener;

/*
 * ################################################################################################
 * #                                                                                              #
 * #     THIS IS A TEMPLATE: COPY THIS FILE AS A STARTING POINT TO IMPLEMENT FURTHER STEPS.       #
 * #                                                                                              # 
 * ################################################################################################
 */

/**
 * This class defines what happens when the above step is executed
 */
class BtcVectorExportStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;

    private BtcVectorExportStep step;

    /*
     * This field can be used to indicate what's happening during the execution
     */
    private StimuliVectorsApi stimuliVectorsApi = new StimuliVectorsApi();
    private RequirementBasedTestCasesApi rbTestCaseApi = new RequirementBasedTestCasesApi();

    /**
     * Constructor
     *
     * @param step
     * @param context
     */
    public BtcVectorExportStepExecution(BtcVectorExportStep step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    /*
     * Put the desired action here:
     * - checking preconditions
     * - access step parameters (field step: step.getFoo())
     * - calling EP Rest API
     * - print text to the Jenkins console (field: jenkinsConsole)
     * - set response code (field: response)
     */
    private ProfilesApi profilesApi = new ProfilesApi();
    private StimuliVectorsApi vectorApi = new StimuliVectorsApi();
    private ArchitecturesApi archApi = new ArchitecturesApi();
    
    @Override
    protected void performAction() throws Exception {
    	// Check preconditions
        try {
            Profile p = profilesApi.getCurrentProfile(); // throws Exception if no profile is active
        } catch (Exception e) {
        	response = 500;
        	error();
        	log("ERROR: could not load a profile. Did you call btcProfileLoad? " + e.getMessage());
            throw new IllegalStateException("You need an active profile for the current command");
        }
        // TODO: EP-2735
        String format = step.getVectorFormat();
        checkArgument(format == "CSV" || format == "Excel", 
        		"Error: valid vectorFormat is csv or excel, not " + format);
        
        //Get all vectors
        String exportDir = toRemoteAbsolutePathString(step.getExportDir());

        // Stimuli Vector  -- Default info.setVectorKind("TC");
        Job job;
        List<B2BStimuliVector> allVectors = null;
        try {
        	allVectors = vectorApi.getStimuliVectors();
        } catch (Exception e) {
        	log("WARNING could not find stimuli vectors: " + e.getMessage());
        	try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        	warning();
        }
        List<String> allVectorNames = new ArrayList<String>();
        for (B2BStimuliVector vector : allVectors) {
        	allVectorNames.add(vector.getUid());
        }

        RestVectorExportDetails r = new RestVectorExportDetails();
        if (step.getVectorFormat() == "Excel") {
        	try {
	        	List<Architecture> architectures = archApi.getArchitectures(null);
	        	r.setArchitectureUid(architectures.get(1).getUid().toString());
	        	r.setSingleFile(false); // TODO once EP-2711 is fixed, make sure 
	        							// that we dont get an error on exporting excel (single-file)
	        	// TODO: EP-2711. this is a temporary workaround. we shouldnt need r.
        	} catch (Exception e) {
        		log("ERROR getting architecture: " + e.getMessage());
        		try {log(((ApiException)e).getResponseBody());} catch (Exception idc) {};
        		error();
        	}
        } else if (step.getVectorFormat() == "CSV") {
        	r.setCsvDelimiter(step.getCsvDelimiter());
        	r.setSingleFile(step.isSingleFile());
        }
        
        // Stimuli Vector
        RestStimuliVectorExportInfo info = new RestStimuliVectorExportInfo();
        info.setExportFormat(step.getVectorFormat());
        info.setExportDirectory(exportDir.toString());
        info.setUiDs(allVectorNames);
        info.additionalOptions(r);
        job = stimuliVectorsApi.exportStimuliVectors(info);
        info("Exported Stimuli Vectors.");
        log("Exported Stimuli Vectors.");
        // TODO: make sure we can detail to directory. if not, only on CSV single-file export.
        // the export linked, i think this is giving a 404 not found
        detailWithLink("Stimuli Vectors Export File", info.getExportDirectory());

        HttpRequester.waitForCompletion(job.getJobID());

        // TODO Questions
        // 1. How should I handle the job from the stimuliVectorsApi.importStimuliVectors(info)?
        // 2. For the Stimuli Vector, do I just need to switch the setVectorKind, or do I need to use another Api?
        // I couldn't find an api for Test Cases

    }

    private String findFileExtension(String format) {
        String fileExtension;
        if (format.equals("TC")) {
            fileExtension = ".tc";
        } else if (format.equals("CSV")) {
            fileExtension = ".csv";
        } else if (format.equals("EXCEL")) {
            fileExtension = ".xlsx";
        } else {
            // Error, format not recognized
            fileExtension = ".tc";
        }
        return fileExtension;
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class BtcVectorExportStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String dir;
    private String vectorFormat;
    private String vectorKind = "TC";
    private String csvDelimiter = "Semicolon";
    private boolean singleFile = true;
    
    @DataBoundConstructor
    public BtcVectorExportStep(String exportDir, String vectorFormat) {
        super();
        this.dir = exportDir;
        this.setVectorFormat(vectorFormat);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BtcVectorExportStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        /*
         * This specifies the step name that the the user can use in his Jenkins Pipeline
         * - for example: btcStartup installPath: 'C:/Program Files/BTC/ep2.9p0', port: 29267
         */
        @Override
        public String getFunctionName() {
            return "btcVectorExport";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Vector Export Step";
        }
    }

    /**
     * Get exportDir.
     * 
     * @return the exportDir
     */
    public String getExportDir() {
        return dir;
    }

    /**
     * Set exportDir.
     * 
     * @param exportDir the exportDir to set
     */
    @DataBoundSetter
    public void setExportDir(String exportDir) {
        this.dir = exportDir;
    }

    /**
     * Get vectorFormat.
     * 
     * @return the vectorFormat
     */
    public String getVectorFormat() {
        return vectorFormat;
    }

    /**
     * Set vectorFormat.
     * 
     * @param vectorFormat the vectorFormat to set
     */
    @DataBoundSetter
    public void setVectorFormat(String vectorFormat) {
        this.vectorFormat = vectorFormat;
    }

    /**
     * Get vectorKind.
     * 
     * @return the vectorKind
     */
    public String getVectorKind() {
        return vectorKind;
    }

    /**
     * Set vectorKind.
     * 
     * @param vectorKind the vectorKind to set
     */
    @DataBoundSetter
    public void setVectorKind(String vectorKind) {
        this.vectorKind = vectorKind;
    }

	public String getCsvDelimiter() {
		return csvDelimiter;
	}

	@DataBoundSetter
	public void setCsvDelimiter(String csvDelimiter) {
		this.csvDelimiter = csvDelimiter;
	}

	public boolean isSingleFile() {
		return singleFile;
	}

	@DataBoundSetter
	public void setSingleFile(boolean singleFile) {
		this.singleFile = singleFile;
	}

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

    /*
     * End of getter/setter section
     */

} // end of step class
