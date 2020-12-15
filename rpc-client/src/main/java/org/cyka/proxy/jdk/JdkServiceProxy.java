package org.cyka.proxy.jdk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.AnnotationProcessor;
import org.cyka.annotation.RpcCaller;
import org.cyka.annotation.RpcService;
import org.cyka.pool.RpcClientConnectionPool;
import org.cyka.proxy.ServiceProxy;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.etcd.EtcdDiscoveryClient;
import org.cyka.registry.lb.LoadBalanceStrategy;
import org.cyka.registry.lb.RoundRobinLoadBalanceStrategy;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class JdkServiceProxy implements ServiceProxy {

  private final RpcClientConnectionPool connectionPool;
  private final DiscoveryClient discoveryClient;
  private final LoadBalanceStrategy strategy;
  private final Set<String> serviceNamesToBeWatched = Sets.newHashSet();

  @Override
  public ServiceProxy servicePackageScan(String basePackage) {
    Set<Class<?>> rpcCallerClasses =
        new AnnotationProcessor(basePackage)
            .getAllAnnotatedClass(RpcCaller.class).stream()
                .filter(callerClass -> callerClass.getAnnotation(RpcService.class) == null)
                .collect(Collectors.toSet());
    serviceNamesToBeWatched.addAll(
        rpcCallerClasses.stream()
            .map(clazz -> clazz.getAnnotation(RpcCaller.class).serviceName())
            .collect(Collectors.toSet()));
    log.debug("service names to watch: {}", serviceNamesToBeWatched);
    discoveryClient.watchServicesChange(serviceNamesToBeWatched);
    generateServiceProxy(rpcCallerClasses);
    return this;
  }

  @Override
  public ServiceProxy generateServiceProxy(Collection<Class<?>> callerServiceClasses) {
    if (Objects.nonNull(callerServiceClasses) && !callerServiceClasses.isEmpty()) {
      for (Class<?> callerClass : callerServiceClasses) {
        RpcCaller callerClassAnnotation = callerClass.getAnnotation(RpcCaller.class);
        checkNotNull(callerClassAnnotation, "this is not a @RpcCaller mark interface");
        String serviceName = callerClassAnnotation.serviceName();
        String version = callerClassAnnotation.version();
        if (!serviceNamesToBeWatched.contains(serviceName)) {
          serviceNamesToBeWatched.add(serviceName);
          this.discoveryClient.watchServicesChange(Lists.newArrayList(serviceName));
        }
        Object instance =
            Proxy.newProxyInstance(
                callerClass.getClassLoader(),
                new Class<?>[] {callerClass},
                new ServiceProxyHandler(
                    serviceName, version, strategy, connectionPool, discoveryClient));
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

  // -----------------------constructor---------------------------------------
  public JdkServiceProxy(RpcClientConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
    // todo: make an option to choose
    this.discoveryClient = new EtcdDiscoveryClient();
    this.strategy = new RoundRobinLoadBalanceStrategy();
  }
}
