package net.ginkgo.server.command;

import net.ginkgo.server.annotation.GinkgoCommand;
import net.ginkgo.server.core.GinkgoTaskCenter;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 查看计划表命令
 */
@GinkgoCommand
public class CommandSchedules extends AbstractCommand{
    public CommandSchedules() {
        super("schedules", "tasks");
    }

    @Override
    public void doCommand(Map<String, String[]> arguments) {
        Map<String, ScheduledFuture<?>> scheduledFutureMap = GinkgoTaskCenter.getCurrentSchedule();
        System.out.println("------------------ Schedules ------------------");
        scheduledFutureMap.forEach((k, v) -> {
            long time = v.getDelay(TimeUnit.MILLISECONDS);
            if(time < 3000){
                System.err.println(k+" -> Will be executed in "+time/1000.0+" sec.");  //Recent
            }else {
                System.out.println(k+" -> Will be executed in "+time/1000.0+" sec.");
            }
        });
        System.out.println("------------------ ————————— ------------------");
    }
}
