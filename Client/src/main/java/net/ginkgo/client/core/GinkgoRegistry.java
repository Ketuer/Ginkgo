package net.ginkgo.client.core;

import net.ginkgo.client.entity.Packet;
import net.ginkgo.client.entity.PacketHandshake;
import net.ginkgo.client.security.AbstractPacketEncryption;
import net.ginkgo.client.security.RSAPacketEncryption;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GinkgoRegistry {
    static final Map<Class<? extends Packet>, Map<Object, Set<Method>>> LISTENER_REGISTER_MAP = new HashMap<>();
    private static final Map<String, Class<? extends Packet>> PACKET_REGISTER_MAP = new HashMap<>();
    private static final Map<String, AbstractPacketEncryption> ENCRYPTION_MAP = new HashMap<>();
    static AbstractPacketEncryption encryption = null;

    public static void setEncryption(PacketHandshake packet){
        switch (packet.getAlgorithm()){
            case "RSA":
                encryption = new RSAPacketEncryption(packet.getKey());
                break;
            default:
                ENCRYPTION_MAP.get(packet.getAlgorithm());
        }
    }

    /**
     * 注册数据加密算法（服务端使用此算法才能使用）
     * @param name 算法名称
     * @param encryption 算法实现
     */
    public static void registerEncryption(String name, AbstractPacketEncryption encryption){
        ENCRYPTION_MAP.putIfAbsent(name, encryption);
    }

    public static void registerHandler(Class<? extends Packet> type, Object listenerObj, Method handleMethod){
        LISTENER_REGISTER_MAP
                .computeIfAbsent(type, k -> new HashMap<>())
                .computeIfAbsent(listenerObj, k -> new HashSet<>())
                .add(handleMethod);
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
}
