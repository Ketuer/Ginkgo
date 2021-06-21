package net.ginkgo.client.entity;

import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

/**
 * 数据包
 * 每个数据包必须包含类型字段，且类型作为数据包的唯一标志！
 */
public abstract class Packet {
    /**
     * 将对象数据打包到字节数据包中
     * @param packer 打包器
     */
    public abstract void write(BufferPacker packer) throws IOException;

    /**
     * 从解包器中读取字节数据包内容
     * @param unpacker 解包器
     */
    public abstract void read(BufferUnpacker unpacker) throws IOException;
}
