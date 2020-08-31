package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.AnnotationProcessor;
import org.cyka.annotation.RpcCaller;
import org.junit.Test;

import java.util.Set;

@RpcCaller(serviceName = "myService" ,version = "test")
@Slf4j
public class ReflectionTest {

  @Test
  public void annotationScanTest() {
    AnnotationProcessor annotationProcessor = new AnnotationProcessor("org.cyka");
    Set<Class<?>> annotatedClass = annotationProcessor.getAllAnnotatedClass(RpcCaller.class);
    log.info("RpcCaller marker classes : {}", annotatedClass);
    for (Class<?> aClass : annotatedClass) {
      log.info("service name: {}", aClass.getAnnotation(RpcCaller.class).serviceName());
      log.info("version : {}", aClass.getAnnotation(RpcCaller.class).version());
    }
  }
}
