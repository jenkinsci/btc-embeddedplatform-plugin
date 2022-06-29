package com.btc.ep.plugins.embeddedplatform.step.basic;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.openapitools.client.api.ProfilesApi;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.ComputerPinger;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

/**
 * This class defines what happens when the above step is executed
 */
class BtcStartupStep2Execution extends SynchronousNonBlockingStepExecution<Void> {

	private static final long serialVersionUID = 1L;
	private BtcStartupStep2 step;

	public BtcStartupStep2Execution(BtcStartupStep2 step, StepContext context) {
		super(context);
		this.step = step;
	}

	@Override
	protected Void run() throws Exception {
		PrintStream jenkinsConsole = getContext().get(TaskListener.class).getLogger();
		jenkinsConsole.println(" ------------------- Option A - Using IP addresses ----------------");
		List<String> addresses = getAddress(jenkinsConsole);
		jenkinsConsole.println("Reachable addresses: " + addresses);
		
		jenkinsConsole.println(" ------------------- Option B - Invoke getenv('HOME') on Agent ----------------");
		String valueOfHomeVarOnController = System.getenv("HOME");
		jenkinsConsole.println(valueOfHomeVarOnController);
		jenkinsConsole.println("");
		String valueOfHomeVariable = getValueOfHomeVariable();
		jenkinsConsole.println(valueOfHomeVariable);
		
		return null;
	}
	
	
	private String getValueOfHomeVariable() throws Exception {
		String home = "";
		Callable<String, Exception> exec = new Callable<String, Exception>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String call() throws Exception {
				return new ProfilesApi().getClass().getName();
			}

			@Override
			public void checkRoles(RoleChecker checker) throws SecurityException {
			}
		};
		Launcher launcher = getContext().get(Launcher.class);
		if (launcher != null) {
			VirtualChannel channel = launcher.getChannel();
			if (channel == null) {
				throw new IllegalStateException("Launcher doesn't support remoting but it is required");
			}
			home = channel.call(exec);
		}
		return home;
	}
	
	
	
	private List<String> getAddress(PrintStream jenkinsConsole) throws Exception {
		// Prepare http connection
		Computer computer = getContext().get(Computer.class);
		List<String> hostNames = computer.getChannel().call(new ListPossibleNames());
		
		jenkinsConsole.println("All possible names of agent " + computer.getName()  + ": " + hostNames);
		
//		String address = null;
		List<String> reachableAddresses = new ArrayList<>();
		for (String hostName : hostNames) {
			InetAddress ia = InetAddress.getByName(hostName);
			if (ComputerPinger.checkIsReachable(ia, 3)) {
//				address = hostName;
				reachableAddresses.add(hostName);
//				break;
			}
		}
//		checkArgument(address != null, "Cannot resolve agent IP address for remote connection.");
		return reachableAddresses;
	}
	
	private static class ListPossibleNames extends MasterToSlaveCallable<List<String>,IOException> {
        /**
         * In the normal case we would use {@link Computer} as the logger's name, however to
         * do that we would have to send the {@link Computer} class over to the remote classloader
         * and then it would need to be loaded, which pulls in {@link Jenkins} and loads that
         * and then that fails to load as you are not supposed to do that. Another option
         * would be to export the logger over remoting, with increased complexity as a result.
         * Instead we just use a logger based on this class name and prevent any references to
         * other classes from being transferred over remoting.
         */
        private static final Logger LOGGER = Logger.getLogger(ListPossibleNames.class.getName());

        public List<String> call() throws IOException {
            List<String> names = new ArrayList<>();

            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni =  nis.nextElement();
                LOGGER.log(Level.FINE, "Listing up IP addresses for {0}", ni.getDisplayName());
                Enumeration<InetAddress> e = ni.getInetAddresses();
                while (e.hasMoreElements()) {
                    InetAddress ia =  e.nextElement();
                    if(ia.isLoopbackAddress()) {
                        LOGGER.log(Level.FINE, "{0} is a loopback address", ia);
                        continue;
                    }

                    if(!(ia instanceof Inet4Address)) {
                        LOGGER.log(Level.FINE, "{0} is not an IPv4 address", ia);
                        continue;
                    }

                    LOGGER.log(Level.FINE, "{0} is a viable candidate", ia);
                    names.add(ia.getHostAddress());
                }
            }
            return names;
        }
        private static final long serialVersionUID = 1L;
    }

}

/**
 * This class defines a step for Jenkins Pipeline including its parameters. When
 * the step is called the related StepExecution is triggered (see the class
 * below this one)
 */
public class BtcStartupStep2 extends Step implements Serializable {

	private static final long serialVersionUID = 1L;


	@DataBoundConstructor
	public BtcStartupStep2() {
		super();
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new BtcStartupStep2Execution(this, context);
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
			return "btcDemo";
		}

		/*
		 * Display name (should be somewhat "human readable")
		 */
		@Override
		public String getDisplayName() {
			return "BTC Demo Step";
		}
	}

	

} // end of step class
