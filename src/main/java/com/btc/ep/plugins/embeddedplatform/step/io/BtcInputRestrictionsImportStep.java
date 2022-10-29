package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.InputRestrictionsApi;
import org.openapitools.client.model.InputRestrictionsFolderObject;

import com.btc.ep.plugins.embeddedplatform.model.DataTransferObject;
import com.btc.ep.plugins.embeddedplatform.step.BtcExecution;
import com.btc.ep.plugins.embeddedplatform.util.StepExecutionHelper;
import com.btc.ep.plugins.embeddedplatform.util.Store;

import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcInputRestrictionsImportStepExecution extends SynchronousStepExecution<Object> {

	private static final long serialVersionUID = 1L;

	private BtcInputRestrictionsImportStep step;


	/**
	 * Constructor
	 *
	 * @param step
	 * @param context
	 */
	public BtcInputRestrictionsImportStepExecution(BtcInputRestrictionsImportStep step, StepContext context) {
		super(context);
		this.step = step;
	}
	
	@Override
	public Object run() {
		PrintStream logger = StepExecutionHelper.getLogger(getContext());
		InputRestrictionsImport exec = new InputRestrictionsImport(logger, getContext(), step);
		
		// transfer applicable global options from Store to the dataTransferObject to be available on the agent
		exec.dataTransferObject.exportPath = Store.exportPath;
		
		// run the step execution part on the agent
		DataTransferObject stepResult = StepExecutionHelper.executeOnAgent(exec, getContext());
		
		// post processing on Jenkins Controller
		StepExecutionHelper.postProcessing(stepResult);
		return null;
	}
}

class InputRestrictionsImport extends BtcExecution {
	
	private BtcInputRestrictionsImportStep step;
	public InputRestrictionsImport(PrintStream logger, StepContext context, BtcInputRestrictionsImportStep step) {
		super(logger, context, step, Store.baseDir);
		this.step = step;
	}

	transient InputRestrictionsApi inputRestrictionsApi = new InputRestrictionsApi();

	@Override
	protected Object performAction() throws Exception {
		// Get the path
		String xmlPath = resolveToString(step.getPath());

		InputRestrictionsFolderObject obj = new InputRestrictionsFolderObject();
		obj.setFilePath(xmlPath.toString());
		try {
			inputRestrictionsApi.importFromFile(obj);
		} catch (Exception e) {
			error();
			log("ERROR on input restrictions import: " + e.getMessage());
			try {
				log(((ApiException) e).getResponseBody());
			} catch (Exception idc) {
			}
			;
		}
		info("Imported input restructions");
		return null;

	}


}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcInputRestrictionsImportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String path;

	@DataBoundConstructor
	public BtcInputRestrictionsImportStep(String path) {
		super();
		this.setPath(path);
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcInputRestrictionsImportStepExecution(this, context);
	}

	/**
	 * Get path.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;

	}

	/**
	 * Set path.
	 * 
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;

	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.singleton(TaskListener.class);
		}

		/*
		 * This specifies the step name that the the user can use in his Jenkins
		 * Pipeline - for example: btcStartup installPath: 'C:/Program
		 * Files/BTC/ep2.9p0', port: 29267
		 */
		@Override
		public String getFunctionName() {
			return "btcInputRestrictionsImport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Input Restrictions Import Step";
		}
	}

	/*
	 * This section contains a getter and setter for each field. The setters need
	 * the @DataBoundSetter annotation.
	 */

	/*
	 * End of getter/setter section
	 */

} // end of step class
