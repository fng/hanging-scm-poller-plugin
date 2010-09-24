package name.wydler.hudson.plugin;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.triggers.SCMTrigger;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Extension
public class HangingSCMPollingTrigger extends ManagementLink {
    @Override
    public String getIconFileName() {
        return "orange-square.gif";
    }

    public String getDisplayName() {
        return "Hanging SCM Polling Triggers";
    }

    @Override
    public String getUrlName() {
        return "hangingScmPollingTriggers";
    }

    @Override
    public String getDescription() {
        return "Display and interrupt hanging SCM polling triggers";
    }

    public Long getStarvationThreshold() {
        return SCMTrigger.STARVATION_THRESHOLD;
    }


    public void doInterrupt(StaplerRequest req, StaplerResponse rsp, @QueryParameter(value = "job") String job) throws IOException, ServletException {
        SCMTrigger.Runner runner = getHangingRunnerForJob(job);
        Thread thread = getThreadForHangingScmPoller(job);
        if (runner == null || thread == null) {
            throw new RuntimeException("No hanging SCM Polling runner found for job " + job);
        } else {
            thread.interrupt();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        rsp.forwardToPreviousPage(req);

    }

    //TODO return functionaljava Option to avoid null
    private SCMTrigger.Runner getHangingRunnerForJob(String job) {
        for (RunnerWithThreads runner : getHangingScmTriggerRunners()) {
            if (runner.getRunner().getTarget().getName().equals(job)) {
                return runner.getRunner();
            }
        }
        return null;
    }

    //TODO return functionaljava Option to avoid null
    private Thread getThreadForHangingScmPoller(String job) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String threadName = thread.getName();
            if (threadName.startsWith("SCM polling for") && threadName.contains(job)) {
                return thread;
            }
        }
        return null;
    }

    private List<Thread> getThreadsForHangingScmRunner(String job) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String threadName = thread.getName();
            if (threadName.startsWith("SCM polling for") && threadName.contains(job)) {
                threads.add(thread);
            }
        }
        return threads;
    }



    public List<RunnerWithThreads> getHangingScmTriggerRunners() {
        SCMTrigger.DescriptorImpl scmTriggerDescriptor = (SCMTrigger.DescriptorImpl) SCMTrigger.all().find(SCMTrigger.class);
        List<SCMTrigger.Runner> runners = scmTriggerDescriptor.getRunners();

        ArrayList<RunnerWithThreads> runnerWithThreadsList = new ArrayList<RunnerWithThreads>();
        for (SCMTrigger.Runner runner : runners) {
            String job = runner.getTarget().getName();
            runnerWithThreadsList.add(new RunnerWithThreads(runner, getThreadsForHangingScmRunner(job)));
        }
        return runnerWithThreadsList;
    }

    //is called to map to any url below hangingScmPollingTriggers
    //e.g. hangingScmPollingTriggers/x -> token = x
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        System.out.println("HangingSCMPollingTrigger.getDynamic token: " + token);
        return null;
    }

    public class RunnerWithThreads{
        private final SCMTrigger.Runner runner;
        private final List<Thread> threads;

        public RunnerWithThreads(SCMTrigger.Runner runner, List<Thread> threads) {
            this.runner = runner;
            this.threads = threads;
        }

        public SCMTrigger.Runner getRunner() {
            return runner;
        }

        public List<Thread> getThreads() {
            return threads;
        }
    }
}
