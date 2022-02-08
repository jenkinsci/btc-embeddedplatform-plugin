package com.btc.ep.plugins.embeddedplatform.step;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.yaml.snakeyaml.Yaml;

import com.btc.ep.plugins.embeddedplatform.step.basic.BtcStartupStep;
import com.btc.ep.plugins.embeddedplatform.util.Util;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;

/**
 * This class defines what happens when the above step is executed
 */
class TestWithBTCStepExecution extends AbstractBtcStepExecution {

    private static final long serialVersionUID = 1L;
    private TestWithBTC step;

    public TestWithBTCStepExecution(TestWithBTC step, StepContext context) {
        super(step, context);
        this.step = step;
    }

    @SuppressWarnings("unchecked")
	@Override
    protected void performAction() throws Exception {
        log("Applying specified test config file " + step.getTestConfigPath());
        
        Path testconfigPath = resolvePath(step.getTestConfigPath());
    	Map<String, Object> testConfig = new Yaml().load(new FileInputStream(testconfigPath.toFile()));
    	System.out.println(testConfig);
        
    	startupOrConnect((Map<String, Object>) testConfig.get("general"));
    	
        // print success message and return response code
        log("--> [200] Example step successfully executed.");
        response = 200;
    }

	private void startupOrConnect(Map<String, Object> generalOptions) throws Exception {
		if (generalOptions == null) {
			new BtcStartupStep().start(getContext()).start();
		} else {
			BtcStartupStep start = new BtcStartupStep();
//			setValueIfPresent(generalOptions, "installPath", ());
			//TODO: create generic way to extract properties from the options and apply them to the step
	        start.setInstallPath("E:/Program Files/BTC/ep2.11p0");
	        start.setPort(29268);
	        start.setAdditionalJvmArgs("-Xmx2g");
	        start.start(getContext()).start();
		}
	}

	private void setValueIfPresent(Map<String, Object> generalOptions, String string, Object object) {
		// TODO Auto-generated method stub
		
	}

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters.
 * When the step is called the related StepExecution is triggered (see the class below this one)
 */
public class TestWithBTC extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Each parameter of the step needs to be listed here as a field
     */
    private String testConfigPath = "TestConfig.yaml";

    @DataBoundConstructor
    public TestWithBTC() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new TestWithBTCStepExecution(this, context);
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
            return "TestWithBTC";
        }

        /*
         * Display name (should be somewhat "human readable")
         */
        @Override
        public String getDisplayName() {
            return "BTC Tests using testconfig";
        }
    }

    /*
     * This section contains a getter and setter for each field. The setters need the @DataBoundSetter annotation.
     */

	public String getTestConfigPath() {
		return testConfigPath;
	}
	@DataBoundSetter
	public void setTestConfigPath(String testConfigPath) {
		this.testConfigPath = testConfigPath;
	}

    /*
     * End of getter/setter section
     */

} // end of step class
