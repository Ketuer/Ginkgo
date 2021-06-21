package com.test.service;

import net.ginkgo.server.annotation.GinkgoService;
import net.ginkgo.server.entity.Session;
import net.ginkgo.server.service.AbstractPacketService;
import net.ginkgo.server.entity.Packet;
import net.ginkgo.server.entity.PacketSimpleMessage;

import java.net.InetSocketAddress;

@GinkgoService(PacketSimpleMessage.class)
public class TestPacketService extends AbstractPacketService {

    @Override
    public Packet receivePacket(Packet packet) {
        Session session = packet.getSession();
        PacketSimpleMessage simple = (PacketSimpleMessage) packet;
        System.out.println("接收收到来自"+session.getAttribute("key")+"客户端的数据包 -> "+simple.getData());
        PacketSimpleMessage back = new PacketSimpleMessage();
        back.setMessage("OK!");
        session.setAttribute("key", "lnwbn");
        return back;
    }

    @Override
    public boolean filter(InetSocketAddress address, Packet packet) {
        return true;
    }
}
