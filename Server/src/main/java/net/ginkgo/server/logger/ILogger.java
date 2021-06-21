package net.ginkgo.server.logger;

public interface ILogger {
    void info(String message);

    void error(Exception e);

    void warn(String message);

    void silent(String log);
}
