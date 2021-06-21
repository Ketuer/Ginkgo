package net.ginkgo.server.config;

import net.ginkgo.server.security.AbstractPacketEncryption;
import net.ginkgo.server.security.RSAPacketEncryption;

public class NetworkConfig {
    private int sessionExpiredTime = 3600;
    private AbstractPacketEncryption encryption = new RSAPacketEncryption();

    public NetworkConfig setSessionExpiredTime(int sessionExpiredTime) {
        this.sessionExpiredTime = sessionExpiredTime;
        return this;
    }

    public NetworkConfig setEncryption(AbstractPacketEncryption encryption) {
        if(encryption == null)
            throw new IllegalArgumentException("Encryption can't be null!");
        this.encryption = encryption;
        return this;
    }

    public int getSessionExpiredTime() {
        return sessionExpiredTime;
    }

    public AbstractPacketEncryption getEncryption() {
        return encryption;
    }
}
