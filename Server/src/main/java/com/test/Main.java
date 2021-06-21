package com.test;

import net.ginkgo.server.annotation.GinkgoPackageScan;
import net.ginkgo.server.annotation.ServerApplication;
import net.ginkgo.server.core.GinkgoStarter;

@GinkgoPackageScan("com.test")
@ServerApplication
public class Main {
    public static void main(String[] args) {
        GinkgoStarter.main(Main.class, args);
    }
}
