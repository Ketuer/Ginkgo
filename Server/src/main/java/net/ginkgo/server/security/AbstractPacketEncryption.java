package net.ginkgo.server.security;

import net.ginkgo.server.entity.PacketHandshake;

public abstract class AbstractPacketEncryption {

    public abstract byte[] encode(byte[] message);

    public abstract byte[] decode(byte[] bytes) ;

    public abstract PacketHandshake handshake();
}
