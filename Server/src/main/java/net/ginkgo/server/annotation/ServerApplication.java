package net.ginkgo.server.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServerApplication {
    String value() default "GinkgoApplication";
    String ip() default "127.0.0.1";
    int port() default 9090;
}
