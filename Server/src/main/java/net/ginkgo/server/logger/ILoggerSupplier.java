package net.ginkgo.server.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

@FunctionalInterface
public interface ILoggerSupplier {

    ILogger getLogger(Class<?> clazz);

}
