package org.cyka.proxy;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.AnnotationProcessor;
import org.cyka.annotation.RpcCaller;
import org.cyka.pool.RpcClientConnectionPool;
import org.cyka.protocol.RpcRequest;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class JdkServiceProxy implements ServiceProxy {

  private final RpcClientConnectionPool connectionPool;
  private final ServiceRegistry serviceRegistry;

  @Override
  public ServiceProxy servicePackageScan(String basePackage) {
    Set<Class<?>> RpcCallerClasses =
        new AnnotationProcessor(basePackage).getAllAnnotatedClass(RpcCaller.class);
    generateServiceProxy(RpcCallerClasses);
    return this;
  }

  @Override
  public ServiceProxy generateServiceProxy(Collection<Class<?>> callerServiceClasses) {
    if (Objects.nonNull(callerServiceClasses) && !callerServiceClasses.isEmpty()) {
      for (Class<?> callerClass : callerServiceClasses) {
        RpcCaller callerClassAnnotation = callerClass.getAnnotation(RpcCaller.class);
        checkNotNull(callerClassAnnotation, "this is not a @RpcCaller marker interface");
        String serviceName = callerClassAnnotation.serviceName();
        String version = callerClassAnnotation.version();
        Object instance =
            Proxy.newProxyInstance(
                callerClass.getClassLoader(),
                new Class<?>[] {callerClass},
                new serviceProxyHandler(serviceName, version));
        log.debug("generate new instance for service class : {}", callerClass);
        serviceProxyMap.put(callerClass, instance);
      }
    }
    return this;
  }

  @Override
  public <T> T getInstance(Class<T> clazz) {
    checkNotNull(clazz, "the class cannot be null");
    return serviceProxyMap.getInstance(clazz);
  }

  public JdkServiceProxy(RpcClientConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
    this.serviceRegistry = new EtcdServiceRegistry();
  }

  class serviceProxyHandler implements InvocationHandler {
    private final String serviceName;
    private final String version;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (Object.class == method.getDeclaringClass()) {
        String name = method.getName();
        switch (name) {
          case "equals":
            return proxy == args[0];
          case "hashCode":
            return System.identityHashCode(proxy);
          case "toString":
            return proxy.getClass().getName()
                + "@"
                + Integer.toHexString(System.identityHashCode(proxy))
                + ", with InvocationHandler "
                + this;
          default:
            throw new IllegalStateException(String.valueOf(method));
        }
      }
      RpcRequest request = new RpcRequest();
      request.setRequestId(UUID.randomUUID().toString());
      request.setClassName(method.getDeclaringClass().getName());
      request.setMethodName(method.getName());
      request.setParameterTypes(method.getParameterTypes());
      request.setParameters(args);
      request.setVersion(version);
      return null;
    }

    serviceProxyHandler(String serviceName, String version) {
      this.serviceName = serviceName;
      this.version = version;
    }
  }
}
