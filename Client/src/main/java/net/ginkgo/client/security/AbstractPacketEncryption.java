package net.ginkgo.client.security;

public abstract class AbstractPacketEncryption {

    public abstract byte[] decode(byte[] bytes);

    public abstract byte[] encode(byte[] message);
}
