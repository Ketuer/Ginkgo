package net.ginkgo.server.entity;


import net.ginkgo.server.annotation.GinkgoPacket;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

/**
 * 握手数据包，包含通信加密算法、密钥、SessionID
 */
@GinkgoPacket
public class PacketHandshake extends Packet{

    String algorithm;
    byte[] key;
    String sessionID;

    public void set(String algorithm, byte[] key, String sessionID){
        this.algorithm = algorithm;
        this.key = key;
        this.sessionID = sessionID;
    }

    public byte[] getKey() {
        return key;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public void write(BufferPacker packer) throws IOException {
        packer.write(algorithm);
        packer.write(key);
        packer.write(sessionID);
    }

    @Override
    public void read(BufferUnpacker unpacker) throws IOException {
        algorithm = unpacker.readString();
        key = unpacker.readByteArray();
        sessionID = unpacker.readString();
    }
}
