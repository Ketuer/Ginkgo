package net.ginkgo.server.service;

import net.ginkgo.server.entity.Packet;

import java.net.InetSocketAddress;

public abstract class AbstractPacketService {

    /**
     * 处理数据包接收动作
     * @param packet 数据包
     * @return 回复数据包
     */
    public Packet receivePacket(Packet packet){
        throw new UnsupportedOperationException("No Impl!");
    }

    /**
     * 自定义数据包过滤器
     * @return 是否放行
     */
    public boolean filter(InetSocketAddress address, Packet packet){
        return true;
    }
}
