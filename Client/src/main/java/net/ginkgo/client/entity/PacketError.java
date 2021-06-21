package net.ginkgo.client.entity;

import net.ginkgo.client.annotation.GinkgoPacket;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

@GinkgoPacket
public class PacketError extends Packet{

    int code;

    public int getCode() {
        return code;
    }

    public PacketError(){}

    public PacketError(int code){
        this.code = code;
    }

    @Override
    public void write(BufferPacker packer) throws IOException {
        packer.write(code);
    }

    @Override
    public void read(BufferUnpacker unpacker) throws IOException {
        code = unpacker.readInt();
    }
}
