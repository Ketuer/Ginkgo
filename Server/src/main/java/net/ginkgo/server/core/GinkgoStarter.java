package net.ginkgo.server.core;

import net.ginkgo.server.annotation.*;
import net.ginkgo.server.command.AbstractCommand;
import net.ginkgo.server.config.AbstractConfiguration;
import net.ginkgo.server.entity.Packet;
import net.ginkgo.server.logger.ILogger;
import net.ginkgo.server.service.AbstractPacketService;

public class GinkgoStarter {
    /**
     * 服务器启动入口类，需要调用此方法启动服务器。
     * @param mainClazz 启动主类
     * @param args 参数
     */
    @SuppressWarnings("unchecked")
    public static void main(Class<?> mainClazz, String... args){
        //进行初始化扫描
        ServerApplication application = mainClazz.getDeclaredAnnotation(ServerApplication.class);
        if(application == null) throw new IllegalStateException(
                "Only with ServerApplication annotation can start the main class as the server!");
        GinkgoPackageScan scan = mainClazz.getDeclaredAnnotation(GinkgoPackageScan.class);
        if(scan != null){
            for (String path : scan.value()) {
                ScanUtil.getClasses(path, true).forEach(c -> {
                    try{
                        if(c.isAnnotationPresent(GinkgoService.class) && c.getSuperclass().equals(AbstractPacketService.class)){
                            GinkgoRegistry.registerPacketService(
                                    c.getDeclaredAnnotation(GinkgoService.class).value(), (AbstractPacketService) c.newInstance());
                        }else if(c.isAnnotationPresent(GinkgoPacket.class) && c.getSuperclass().equals(Packet.class)){
                            GinkgoRegistry.registerPacketType((Class<? extends Packet>) c);
                        }else if(c.isAnnotationPresent(GinkgoCommand.class) && c.getSuperclass().equals(AbstractCommand.class)){
                            GinkgoRegistry.registerCommand((AbstractCommand) c.newInstance());
                        }else if(c.isAnnotationPresent(GinkgoConfig.class) && c.getSuperclass().equals(AbstractConfiguration.class)){
                            AbstractConfiguration configuration = (AbstractConfiguration) c.newInstance();
                            configuration.configureNetwork(GinkgoConfiguration.networkConfig);
                            configuration.configure(GinkgoConfiguration.globalConfig);
                        }
                    }catch (IllegalAccessException | InstantiationException e){
                        e.printStackTrace();
                    }
                });
            }
        }

        ILogger logger = GinkgoRegistry.getLogger(GinkgoStarter.class);
        logger.info("Set logger to "+logger.getClass());
        registerDefaults();
        logger.info("The configuration is loaded and the server is being started...");
        GinkgoServerRunner.runServer(application);  //启动服务器
    }

    /**
     * 载入项目内置的模块
     */
    @SuppressWarnings("unchecked")
    private static void registerDefaults(){
        ScanUtil.getClasses("net.ginkgo.server.entity", true).forEach(c -> {
            if(c.isAnnotationPresent(GinkgoPacket.class) && c.getSuperclass().equals(Packet.class)){
                GinkgoRegistry.registerPacketType((Class<? extends Packet>) c);
            }
        });

        //框架内置命令
        ScanUtil.getClasses("net.ginkgo.server.command", true).forEach(c -> {
            if(c.isAnnotationPresent(GinkgoCommand.class) && c.getSuperclass().equals(AbstractCommand.class)){
                try {
                    GinkgoRegistry.registerCommand((AbstractCommand) c.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
