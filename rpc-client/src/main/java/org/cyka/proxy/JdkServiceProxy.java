package org.cyka.proxy;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.AnnotationProcessor;
import org.cyka.annotation.RpcCaller;
import org.cyka.annotation.RpcService;
import org.cyka.async.AsyncResult;
import org.cyka.async.CompletableResult;
import org.cyka.constant.ClientAttribute;
import org.cyka.exception.ServiceCallException;
import org.cyka.pool.RpcClientConnectionPool;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.etcd.EtcdDiscoveryClient;
import org.cyka.registry.lb.LoadBalanceStrategy;
import org.cyka.registry.lb.RoundRobinLoadBalanceStrategy;
import org.cyka.registry.lb.RpcLoadBalance;

import javax.management.ServiceNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class JdkServiceProxy implements ServiceProxy {

  private final RpcClientConnectionPool connectionPool;
  private final DiscoveryClient discoveryClient;
  private final RpcLoadBalance rpcLoadBalance;
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
        checkNotNull(callerClassAnnotation, "this is not a @RpcCaller marker interface");
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

  // -----------------------constructor---------------------------------------
  public JdkServiceProxy(RpcClientConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
    // todo: make an option to choose
    this.discoveryClient = new EtcdDiscoveryClient();
    this.rpcLoadBalance = new RpcLoadBalance(discoveryClient);
    this.strategy = new RoundRobinLoadBalanceStrategy();
  }

  // ------------------------release channel listener-------------------------
  class channelReleaseListener implements ChannelFutureListener {
    private final ServiceEndpoint endpoint;

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      // TODO: 2020/9/1 check request send status
      // return the channel to pool
      if (future.isDone()) {
        Channel channel = future.channel();
        connectionPool.releaseChannel(
            endpoint,
            channel,
            (emptyValue, releaseException) ->
                releaseException.ifPresent(
                    e ->
                        log.warn(
                            "channel : {} release occur error, reason : {}",
                            channel.id(),
                            e.getMessage())));
      }
    }

    public channelReleaseListener(ServiceEndpoint endpoint) {
      this.endpoint = endpoint;
    }
  }

  // ----------------------Jdk InvocationHandler  implementation-------------------------
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
      RpcRequest rpcRequest = generateRpcRequest(method, args);
      AsyncResult<RpcResponse> responseAsyncResult = new CompletableResult<>(null);
      ServiceEndpoint serviceEndpoint = getServiceEndpointByStrategy(serviceName, strategy);
      connectionPool.acquireChannel(
          serviceEndpoint,
          (channel, throwable) -> {
            // check exception
            if (!throwable.isPresent()) {
              ConcurrentMap<String, AsyncResult<RpcResponse>> responseCallbackMap =
                  channel.attr(ClientAttribute.RESPONSE_CALLBACK_MAP).get();
              responseCallbackMap.computeIfAbsent(
                  rpcRequest.getRequestId(), (key) -> responseAsyncResult);
              channel
                  .writeAndFlush(rpcRequest)
                  .addListener(new channelReleaseListener(serviceEndpoint));
            } else {
              throwable.get().printStackTrace();
            }
          });
      responseAsyncResult.await();
      RpcResponse response = responseAsyncResult.getValue();
      if (!Strings.isNullOrEmpty(response.getError())) {
        throw new ServiceCallException(response.getError());
      }
      return method.getReturnType().cast(response.getResult());
    }

    private RpcRequest generateRpcRequest(Method method, Object[] args) {
      RpcRequest request = new RpcRequest();
      request.setRequestId(UUID.randomUUID().toString()); // TODO: 2020/9/1 make unique request id
      request.setClassName(method.getDeclaringClass().getName());
      request.setMethodName(method.getName());
      request.setParameterTypes(method.getParameterTypes());
      request.setParameters(args);
      request.setVersion(version);
      return request;
    }

    private ServiceEndpoint getServiceEndpointByStrategy(
        String serviceName, LoadBalanceStrategy strategy) throws ServiceNotFoundException {
      return rpcLoadBalance.chooseService(serviceName, strategy);
    }

    serviceProxyHandler(String serviceName, String version) {
      this.serviceName = serviceName;
      this.version = version;
    }
  }
}
