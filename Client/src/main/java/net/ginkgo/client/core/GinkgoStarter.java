package net.ginkgo.client.core;

import net.ginkgo.client.annotation.*;
import net.ginkgo.client.entity.Packet;

import java.lang.reflect.Method;

public class GinkgoStarter {

    public static Thread NETWORK_THREAD;

    @SuppressWarnings("unchecked")
    public static void main(Class<?> mainClazz, String... args){
        ClientApplication application = mainClazz.getDeclaredAnnotation(ClientApplication.class);
        if(application == null) throw new IllegalStateException(
                "Only with ServerApplication annotation can start the main class as the server!");

        GinkgoPackageScan scan = mainClazz.getDeclaredAnnotation(GinkgoPackageScan.class);
        if(scan != null){
            for (String path : scan.value()) {
                ScanUtil.getClasses(path, true).forEach(c -> {
                    try{
                        if(c.isAnnotationPresent(GinkgoListener.class)){
                            Object instance = c.newInstance();
                            for (Method method : c.getDeclaredMethods()) {
                                GinkgoPacketHandler handler = method.getDeclaredAnnotation(GinkgoPacketHandler.class);
                                Class<? extends Packet> clazz = handler.value();
                                if(method.isAnnotationPresent(GinkgoPacketHandler.class)){
                                    if(method.getParameterTypes().length != 1 || !method.getParameterTypes()[0].equals(clazz))
                                        throw new IllegalArgumentException("Listener handle method can only have one argument that extend by Packet class!");
                                    GinkgoRegistry.registerHandler(clazz, instance, method);
                                }
                            }
                        }else if(c.isAnnotationPresent(GinkgoPacket.class) && c.getSuperclass().equals(Packet.class)){
                            GinkgoRegistry.registerPacketType((Class<? extends Packet>) c);
                        }
                    }catch (IllegalAccessException | InstantiationException e){
                        e.printStackTrace();
                    }
                });
            }
        }

        registerDefaults();

        NETWORK_THREAD = new Thread(() -> GinkgoClientRunner.runClient(application));
        NETWORK_THREAD.start();
    }

    /**
     * 载入项目内置的模块
     */
    @SuppressWarnings("unchecked")
    private static void registerDefaults(){
        ScanUtil.getClasses("net.ginkgo.client.entity", true).forEach(c -> {
            if(c.isAnnotationPresent(GinkgoPacket.class) && c.getSuperclass().equals(Packet.class)){
                GinkgoRegistry.registerPacketType((Class<? extends Packet>) c);
            }
        });
    }
}
