package org.cyka.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.cyka.server.ServiceBeanRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
  private final ServiceBeanRegistry beanRegistry;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
    if (RpcRequest.HEART_BEAT_REQUEST.getRequestId().equalsIgnoreCase(request.getRequestId())) {
      log.info("Server read heartbeat ping from channel : {}", ctx.channel().id());
      return;
    }
    // TODO: 2020/9/1 execute on other thread pool
    ctx.executor().execute(() -> handleRpcRequest(ctx, request));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    super.userEventTriggered(ctx, evt);
  }

  private void handleRpcRequest(ChannelHandlerContext ctx, RpcRequest request) {
    RpcResponse rpcResponse = new RpcResponse();
    rpcResponse.setRequestId(request.getRequestId());
    Class<?> targetClazz;
    Object targetBean;
    // find the target class and bean
    try {
      targetClazz = Class.forName(request.getClassName());
      targetBean = beanRegistry.getServiceBean(targetClazz);
    } catch (ClassNotFoundException e) {
      sendErrorResponse(ctx, rpcResponse, e.getMessage());
      return;
    }
    // check target bean
    if (Objects.isNull(targetBean)) {
      sendErrorResponse(
          ctx,
          rpcResponse,
          "Server: "
              + ctx.channel().localAddress()
              + "do not implement the service: "
              + targetClazz);
      return;
    }
    // invoke the target bean's method
    Class<?> targetBeanClass = targetBean.getClass();
    log.debug("target bean class : {}", targetBeanClass);
    String methodName = request.getMethodName();
    Class<?>[] parameterTypes = request.getParameterTypes();
    Object[] parameters = request.getParameters();
    try {
      Method method = targetBeanClass.getMethod(methodName, parameterTypes);
      method.setAccessible(true);
      rpcResponse.setResult(method.invoke(targetBean, parameters));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      sendErrorResponse(ctx, rpcResponse, e.getMessage());
      return;
    }
    ctx.writeAndFlush(rpcResponse);
  }

  private void sendErrorResponse(
      ChannelHandlerContext ctx, RpcResponse rpcResponse, String errorMessage) {
    rpcResponse.setError(errorMessage);
    ctx.writeAndFlush(rpcResponse);
  }

  public RpcServerHandler(ServiceBeanRegistry beanRegistry) {
    this.beanRegistry = beanRegistry;
  }
}
