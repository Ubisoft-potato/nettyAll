package org.cyka.proxy.jdk;

import com.google.common.base.Strings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.cyka.async.AsyncResult;
import org.cyka.async.CompletableResult;
import org.cyka.constant.ClientAttribute;
import org.cyka.exception.ServiceCallException;
import org.cyka.pool.RpcClientConnectionPool;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.lb.LoadBalanceStrategy;
import org.cyka.registry.lb.RpcLoadBalancer;

import javax.management.ServiceNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author long
 * @version 1.0.0
 * @since 2020/12/14 16:20
 */
@Slf4j
class ServiceProxyHandler implements InvocationHandler {

  private static final Class<?> ASYNC_TYPE = AsyncResult.class;

  private final String serviceName;
  private final String version;
  private final LoadBalanceStrategy strategy;
  private final RpcClientConnectionPool connectionPool;
  private final RpcLoadBalancer rpcLoadBalancer;

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (Object.class == method.getDeclaringClass()) return invokeObjectMethod(proxy, method, args);
    RpcRequest rpcRequest = generateRpcRequest(method, args);
    AsyncResult<RpcResponse> responseAsyncResult = new CompletableResult<>();
    Class<?> returnType = method.getReturnType();
    // normal block request
    doRequest(rpcRequest, responseAsyncResult);
    responseAsyncResult.await();
    RpcResponse response = responseAsyncResult.getValue();
    if (!Strings.isNullOrEmpty(response.getError())) {
      throw new ServiceCallException(response.getError());
    }
    return returnType.cast(response.getResult());
  }

  private void doRequest(RpcRequest rpcRequest, AsyncResult<RpcResponse> responseAsyncResult)
      throws ServiceNotFoundException {
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
  }

  private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
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

  /**
   * @param method - the method to call
   * @param args - method args
   * @return generated rpcRequest object
   */
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
    return rpcLoadBalancer.chooseService(serviceName, strategy);
  }

  ServiceProxyHandler(
      String serviceName,
      String version,
      LoadBalanceStrategy strategy,
      RpcClientConnectionPool connectionPool,
      DiscoveryClient discoveryClient) {
    this.serviceName = serviceName;
    this.version = version;
    this.strategy = strategy;
    this.connectionPool = connectionPool;
    this.rpcLoadBalancer = new RpcLoadBalancer(discoveryClient);
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
}
