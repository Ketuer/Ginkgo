package net.ginkgo.server.core;

import net.ginkgo.server.logger.ILogger;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 任务中心，异步任务和计划任务
 */
public class GinkgoTaskCenter {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(10);
    private static final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public static void startTaskCenter(){
        while (true){
            try {
                TimeUnit.SECONDS.sleep(10);
                if(futures.isEmpty()) continue;
                clearUseless();
            } catch (InterruptedException e) {
                ILogger logger = GinkgoRegistry.getLogger(GinkgoTaskCenter.class);
                SCHEDULER.shutdownNow();
                logger.info("Task center has close!");
                break;  //When interrupted
            }
        }
    }

    public static boolean addDelaySchedule(Runnable task, String name, long delay, TimeUnit unit){
        ILogger logger = GinkgoRegistry.getLogger(GinkgoTaskCenter.class);
        if(name.contains(" "))
            throw new IllegalArgumentException("Task name can't contain space!");
        clearUseless();
        if(futures.containsKey(name)) return false;
        futures.put(name, SCHEDULER.schedule(task, delay, unit));
        logger.info("Task: '"+name+"' has been established!");
        return true;
    }

    public static boolean addFixRateSchedule(Runnable task, String name, long delay, long rate, TimeUnit unit){
        ILogger logger = GinkgoRegistry.getLogger(GinkgoTaskCenter.class);
        if(name.contains(" "))
            throw new IllegalArgumentException("Task name can't contain space!");
        clearUseless();
        if(futures.containsKey(name)) return false;
        futures.put(name, SCHEDULER.scheduleAtFixedRate(task, delay, rate, unit));
        logger.info("Fix rate task: '"+name+"' has been established!");
        return true;
    }

    /**
     * 尝试取消指定名称的任务（只是尝试）
     * @param name 名称
     * @return 是否取消成功
     */
    public static boolean cancelSchedule(String name){
        ScheduledFuture<?> future = futures.get(name);
        ILogger logger = GinkgoRegistry.getLogger(GinkgoTaskCenter.class);
        if(future != null) {
            if(future.cancel(true)) {
                futures.remove(name);
                logger.info("Task: "+name+" successfully cancelled!");
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试取消所有的计划任务（只是尝试）
     */
    public static void cancelAllSchedule(){
        ILogger logger = GinkgoRegistry.getLogger(GinkgoTaskCenter.class);
        futures.forEach((k, v) -> {
            if(v.cancel(false)){
                logger.info("Task: "+k+" is successfully cancelled!");
            }else {
                logger.warn("Task: "+k+" can't be cancelled!");
            }
        });
        futures.clear();
    }

    public static Map<String, ScheduledFuture<?>> getCurrentSchedule(){
        clearUseless();
        return futures;
    }

    private static void clearUseless(){
        futures.forEach((k, v) -> {
            if(v.isDone() || v.isCancelled())
                futures.remove(k); //Clean the futures.
        });
    }
}
