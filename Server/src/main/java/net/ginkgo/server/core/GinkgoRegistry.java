package net.ginkgo.server.core;

import net.ginkgo.server.command.AbstractCommand;
import net.ginkgo.server.entity.Packet;
import net.ginkgo.server.logger.ILogger;
import net.ginkgo.server.logger.ILoggerSupplier;
import net.ginkgo.server.security.AbstractPacketEncryption;
import net.ginkgo.server.service.AbstractPacketService;

import java.util.*;

/**
 * 管理中心，包含所有的注册
 */
public class GinkgoRegistry {

    private static final Map<Class<? extends Packet>, Set<AbstractPacketService>> PACKET_SERVICE_MAP = new HashMap<>();
    private static final Map<String, Class<? extends Packet>> PACKET_REGISTER_MAP = new HashMap<>();
    private static final Map<String, AbstractCommand> COMMAND_REGISTER_MAP = new HashMap<>();

    public static AbstractPacketEncryption getEncryption() {
        return GinkgoConfiguration.networkConfig.getEncryption();
    }

    public static void registerCommand(AbstractCommand command){
        COMMAND_REGISTER_MAP.put(command.getKey(), command);
    }

    public static AbstractCommand matchCommand(String input){
        return COMMAND_REGISTER_MAP.computeIfAbsent(input, key -> {
            for (AbstractCommand value : COMMAND_REGISTER_MAP.values()) {
                if(value.match(key)) return value;
            }
            return null;
        });
    }

    public static void registerPacketService(Class<? extends Packet> type, AbstractPacketService service){
        PACKET_SERVICE_MAP
                .computeIfAbsent(type, k -> new HashSet<>())
                .add(service);
    }

    public static Set<AbstractPacketService> dispatchPacket(Class<? extends Packet> clazz){
        return PACKET_SERVICE_MAP.getOrDefault(clazz, Collections.emptySet());
    }

    public static Class<? extends Packet> convertPacket(String type){
        return PACKET_REGISTER_MAP.get(type);
    }

    public static void registerPacketType(Class<? extends Packet> clazz){
        try {
            clazz.getDeclaredConstructor();
            PACKET_REGISTER_MAP.putIfAbsent(clazz.getSimpleName(), clazz);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Couldn't register 'Packet' class "+clazz.getName()+
                    ", Packet class must have a no arguments constructor!");
        }
    }

    public static ILogger getLogger(Class<?> clazz) {
        ILoggerSupplier loggerSupplier = GinkgoConfiguration.globalConfig.getSupplier();
        if(loggerSupplier == null) throw new IllegalStateException("The log system has not been initialized yet!");
        return loggerSupplier.getLogger(clazz);
    }
}
