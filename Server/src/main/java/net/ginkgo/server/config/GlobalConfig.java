package net.ginkgo.server.config;

import net.ginkgo.server.logger.DefaultLogger;
import net.ginkgo.server.logger.ILoggerSupplier;

public class GlobalConfig {
    ILoggerSupplier supplier = DefaultLogger::new;

    public void setLoggerSupplier(ILoggerSupplier supplier) {
        this.supplier = supplier;
    }

    public ILoggerSupplier getSupplier() {
        return supplier;
    }
}
