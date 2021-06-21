package net.ginkgo.client.annotation;

import net.ginkgo.client.entity.Packet;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GinkgoPacketHandler {
    Class<? extends Packet> value();
}
