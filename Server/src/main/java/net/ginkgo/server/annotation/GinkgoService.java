package net.ginkgo.server.annotation;

import net.ginkgo.server.entity.Packet;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GinkgoService {

    Class<? extends Packet> value();
}
