package net.ginkgo.server.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultLogger implements ILogger {

    final Class<?> clazz;

    public DefaultLogger(Class<?> clazz){
        if(clazz == null) throw new IllegalArgumentException("Class type can't be null!");
        this.clazz = clazz;
    }

    private String prefix(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return String.format("%s [%s][%s] ", format.format(date), clazz.getName(), Thread.currentThread().getName());
    }

    @Override
    public void info(String message) {
        System.out.println(this.prefix()+message);
    }

    @Override
    public void error(Exception e) {
        System.err.println(this.prefix()+e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void warn(String message) {
        System.err.println(this.prefix()+message);
    }

    @Override
    public void silent(String log) {
        //Default Logger do no save!
    }
}
