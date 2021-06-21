package net.ginkgo.server.entity;

public interface Session {
    String getID();

    void setAttribute(String key, Object object);

    Object getAttribute(String key);

    boolean hasAttribute(String key);
}
