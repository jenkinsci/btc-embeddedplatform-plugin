package com.btc.ep.plugins.embeddedplatform.step.io;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.InputRestrictionsApi;
import org.openapitools.client.model.InputRestrictionsFolderObject;


import hudson.Extension;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class BtcInputRestrictionsExportStepExecution extends StepExecution {

	private static final long serialVersionUID = 1L;

	private BtcInputRestrictionsExportStep step;

	/*
	 * This field can be used to indicate what's happening during the execution
	 */
	private InputRestrictionsApi inputRestrictionsApi = new InputRestrictionsApi();

	/**
	 * Constructor
	 *
	 * @param step
	 * @param context
	 */
	public BtcInputRestrictionsExportStepExecution(BtcInputRestrictionsExportStep step, StepContext context) {
		super(context);
		this.step = step;
	}

//	@Override
//	protected void performAction() throws Exception {
//		String path = toRemoteAbsolutePathString(step.getPath());
//
//		InputRestrictionsFolderObject file = new InputRestrictionsFolderObject();
//		file.setFilePath(path);
//		try {
//			inputRestrictionsApi.exportToFile(file);
//			detailWithLink("Input Restrictions Export File", file.getFilePath());
//		} catch (ApiException e) {
//			// TODO: convenience workaround EP-2722
//			log("Error: most likely " + step.getPath() + " already exists. Please delete it to continue.");
//			try {
//				log(((ApiException) e).getResponseBody());
//			} catch (Exception idc) {
//			}
//			;
//			error();
//		}
//		info("Finished exporting Input Restrictions");
//
//	}

	@Override
	public boolean start() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcInputRestrictionsExportStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * Each parameter of the step needs to be listed here as a field
	 */
	private String path;

	@DataBoundConstructor
	public BtcInputRestrictionsExportStep() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcInputRestrictionsExportStepExecution(this, context);
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
	@DataBoundSetter
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
			return "btcInputRestrictionsExport";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Input Restrictions Export Step";
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