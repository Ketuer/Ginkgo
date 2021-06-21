package net.ginkgo.server.command;

import net.ginkgo.server.annotation.GinkgoCommand;
import net.ginkgo.server.core.GinkgoRegistry;
import net.ginkgo.server.core.GinkgoServerRunner;
import net.ginkgo.server.core.GinkgoTaskCenter;
import net.ginkgo.server.logger.ILogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 关闭服务器命令
 * * stop 直接关闭服务器
 * * stop -s [seconds] 延时关闭服务器
 */
@GinkgoCommand
public class CommandStop extends AbstractCommand implements Confirmable{

    public CommandStop() {
        super("stop", "shutdown", "end", "exit");
    }

    @Override
    public void doCommand(Map<String, String[]> arguments) {
        ILogger logger = GinkgoRegistry.getLogger(CommandStop.class);
        int delay = 0;
        if(arguments.containsKey("s")){
            String[] strings = arguments.get("s");
            if(strings.length > 0){
                delay = Integer.parseInt(strings[0]);
            }
        }
        if(delay == 0) GinkgoTaskCenter.cancelSchedule("StopServer");  //为 0 时强制清除延时任务并执行
        if(GinkgoTaskCenter.addDelaySchedule(GinkgoServerRunner::stopServer, "StopServer", delay, TimeUnit.SECONDS)){
            if(delay > 0)
                logger.info("A scheduled task has been created, and the server will shutdown in "+delay+" seconds!");
        }else {
            logger.warn("Unable to create a scheduled task, a task with the same name already exists!");
        }
    }

    @Override
    public String message() {
        return "Executing the 'stop' command will try to forcibly terminate all ongoing tasks. Are you sure you want to do this? y/N";
    }

    @Override
    public boolean confirm(String str) {
        return str.equalsIgnoreCase("y");
    }
}
