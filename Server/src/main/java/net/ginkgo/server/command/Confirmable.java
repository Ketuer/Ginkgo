package net.ginkgo.server.command;

public interface Confirmable {

    String message();

    boolean confirm(String str);
}
