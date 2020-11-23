package org.cyka.server;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.AnnotationProcessor;
import org.cyka.annotation.RpcService;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ServiceBeanRegistry {
  private final ClassToInstanceMap<Object> serviceBeanMap = MutableClassToInstanceMap.create();

  public <T> T getServiceBean(Class<T> clazz) {
    return serviceBeanMap.getInstance(clazz);
  }

  public Collection<String> getAllRegisteredServiceName() {
    return serviceBeanMap.values().stream()
        .map(ServiceBeanRegistry::apply)
        .distinct()
        .collect(Collectors.toList());
  }

  public ServiceBeanRegistry(String basePackage) {
    Set<Class<?>> serviceBeanClasses =
        new AnnotationProcessor(basePackage).getAllAnnotatedClass(RpcService.class);
    serviceBeanClasses.forEach(
        serviceBeanClass -> {
          try {
            Object instance = serviceBeanClass.newInstance();
            for (Class<?> serviceInterface : serviceBeanClass.getInterfaces()) {
              serviceBeanMap.put(serviceInterface, instance);
            }
          } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
          }
        });
    log.debug("generated service bean map: {}", serviceBeanMap);
  }

  private static String apply(Object obj) {
    return obj.getClass().getAnnotation(RpcService.class).serviceName();
  }
}
