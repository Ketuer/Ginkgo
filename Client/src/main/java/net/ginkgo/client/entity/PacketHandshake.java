package net.ginkgo.client.entity;


import net.ginkgo.client.annotation.GinkgoPacket;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

@GinkgoPacket
public class PacketHandshake extends Packet{

    String algorithm;
    byte[] key;
    String sessionID;

    public void set(String algorithm, byte[] key){
        this.algorithm = algorithm;
        this.key = key;
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
