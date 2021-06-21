package net.ginkgo.server.entity;

import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.IOException;

/**
 * 数据包
 * <p>
 * 每个数据包必须包含类型字段，且类型作为数据包的唯一标志！每个数据包
 * 都携带了一个Session信息，用于区别用户。
 */
public abstract class Packet {

    Session session;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

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
