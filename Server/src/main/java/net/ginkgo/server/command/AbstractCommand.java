package net.ginkgo.server.command;

import java.util.Map;

public abstract class AbstractCommand {
    String key;
    String[] alias;

    protected AbstractCommand(String key, String... alias){
        this.key = key;
        this.alias = alias;
    }

    public String getKey() {
        return key;
    }

    public abstract void doCommand(Map<String, String[]> arguments) throws Exception;

    public boolean match(String input){
        for (String s : alias) {
            if(s.equals(input)) return true;
        }
        return key.equals(input);
    }
}
