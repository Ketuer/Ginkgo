package net.ginkgo.server.command;

import net.ginkgo.server.annotation.GinkgoCommand;
import net.ginkgo.server.core.GinkgoRegistry;
import net.ginkgo.server.core.GinkgoTaskCenter;
import net.ginkgo.server.logger.ILogger;

import java.util.Arrays;
import java.util.Map;

@GinkgoCommand
public class CommandCancelTask extends AbstractCommand{
    public CommandCancelTask() {
        super("cancel");
    }

    @Override
    public void doCommand(Map<String, String[]> arguments) {
        ILogger logger = GinkgoRegistry.getLogger(CommandCancelTask.class);
        int count = 0;
        if(arguments.containsKey("all")){
            logger.info("Request cancellation of all scheduled tasks, starting...");
            GinkgoTaskCenter.cancelAllSchedule();
            logger.info("Cancel task completed!");
        }else if(arguments.containsKey("n")){
            String[] strings = arguments.get("n");
            logger.info("Request to cancel the task list: "+ Arrays.toString(strings));
            for (String string : strings) {
                if(GinkgoTaskCenter.cancelSchedule(string)) count++;
            }
            logger.info("Request to cancel "+strings.length+" tasks, successfully cancel "+count+" tasks");
        }
    }
}
