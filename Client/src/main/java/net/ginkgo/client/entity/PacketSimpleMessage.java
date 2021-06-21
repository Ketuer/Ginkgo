package net.ginkgo.client.entity;

import net.ginkgo.client.annotation.GinkgoPacket;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

/**
 * 简单消息数据包
 */
@GinkgoPacket
public class PacketSimpleMessage extends Packet {

    private String message;

    public String getData() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void write(BufferPacker packer) throws IOException{
        packer.write(message);
    }

    @Override
    public void read(BufferUnpacker unpacker) throws IOException {
        message = unpacker.readString();
    }
}
