package com.test.config;

import net.ginkgo.server.annotation.GinkgoConfig;
import net.ginkgo.server.config.AbstractConfiguration;
import net.ginkgo.server.config.NetworkConfig;

@GinkgoConfig
public class TestConfig extends AbstractConfiguration {
    @Override
    public void configureNetwork(NetworkConfig config) {

    }
}
