package net.ginkgo.client.core;

import net.ginkgo.client.entity.Packet;
import net.ginkgo.client.security.AbstractPacketEncryption;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 网络数据包处理中心，一般用于发送数据包。
 */
public class GinkgoNetwork {
    private static final MessagePack pack = new MessagePack();
    private static String sessionID = null;

    /**
     * 发送数据包
     * @param packet 数据包
     */
    public static void sendPacket(Packet packet){
        if(sessionID == null) throw new IllegalStateException("No session!");
        AbstractPacketEncryption encryption = GinkgoRegistry.encryption;
        BufferPacker packer = pack.createBufferPacker();
        try {
            packer.write(packet.getClass().getSimpleName());
            packer.write(sessionID);
            packet.write(packer);

            byte[] bytes = packer.toByteArray();
            if(encryption != null) bytes = encryption.encode(bytes); //Encryption data

            bytes = zip(bytes);
            if(bytes == null) return;
            GinkgoClientRunner.socket.register(GinkgoClientRunner.selector,
                    SelectionKey.OP_WRITE, ByteBuffer.wrap(bytes));
            GinkgoClientRunner.selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接受数据包
     * @param packet 数据包
     */
    public static void receivePacket(Packet packet){
        GinkgoRegistry.LISTENER_REGISTER_MAP.get(packet.getClass()).forEach((k, v) -> v.forEach(method -> {
            try {
                method.invoke(k, packet);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }));
    }

    static void setSessionID(String sessionID) {
        GinkgoNetwork.sessionID = sessionID;
    }

    /**
     * Zip压缩数据
     * @param data 数据
     * @return 压缩数据
     */
    private static byte[] zip(byte[] data){
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bos)){
            ZipEntry entry = new ZipEntry("zip");
            entry.setSize(data.length);
            zip.putNextEntry(entry);
            zip.write(data);
            zip.closeEntry();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
