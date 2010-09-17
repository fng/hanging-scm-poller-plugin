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
        }

        rsp.forwardToPreviousPage(req);

    }

    //TODO return functionaljava Option to avoid null
    private SCMTrigger.Runner getHangingRunnerForJob(String job) {
        for (SCMTrigger.Runner runner : getHangingScmTriggerRunners()) {
            if (runner.getTarget().getName().equals(job)) {
                return runner;
            }
        }
        return null;
    }

    //TODO return functionaljava Option to avoid null
    private Thread getThreadForHangingScmPoller(String job) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String threadName = thread.getName();
            if (threadName.startsWith("SCM polling for") && threadName.endsWith("[" + job + "]")) {
                return thread;
            }
        }
        return null;
    }


    public List<SCMTrigger.Runner> getHangingScmTriggerRunners() {
        SCMTrigger.DescriptorImpl scmTriggerDescriptor = (SCMTrigger.DescriptorImpl) SCMTrigger.all().find(SCMTrigger.class);
        return scmTriggerDescriptor.getRunners();
    }

    //is called to map to any url below hangingScmPollingTriggers
    //e.g. hangingScmPollingTriggers/x -> token = x
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        System.out.println("HangingSCMPollingTrigger.getDynamic token: " + token);
        return null;
    }
}
