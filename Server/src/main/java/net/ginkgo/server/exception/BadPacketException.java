package net.ginkgo.server.exception;

public class BadPacketException extends Exception {
    public BadPacketException(String text){
        super(text);
    }
}
