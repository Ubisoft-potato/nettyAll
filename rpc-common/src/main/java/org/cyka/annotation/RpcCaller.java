package org.cyka.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcCaller {
  String serviceName();

  String version() default "original";
}
