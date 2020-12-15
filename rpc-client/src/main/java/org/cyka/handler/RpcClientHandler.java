package org.cyka.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.cyka.async.AsyncResult;
import org.cyka.constant.ClientAttribute;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;

import java.util.concurrent.ConcurrentMap;

@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
    log.debug("receive response from server :{}", ctx.channel().remoteAddress());
    ConcurrentMap<String, AsyncResult<RpcResponse>> responseMap =
        ctx.channel().attr(ClientAttribute.RESPONSE_CALLBACK_MAP).get();
    responseMap.get(rpcResponse.getRequestId()).setValue(rpcResponse);
    responseMap.remove(rpcResponse.getRequestId());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.warn(
        "channel: {} occur error: {} , with local address: {} , remote address: {}",
        ctx.channel().id(),
        cause.getMessage(),
        ctx.channel().localAddress(),
        ctx.channel().remoteAddress());
    super.exceptionCaught(ctx, cause);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      final Channel channel = ctx.channel();
      log.debug(
          "channel: {} --> remote : {}, is reach idleTime, send heart beat request",
          channel.remoteAddress(),
          channel.id());
      if (channel.isActive()) ctx.writeAndFlush(RpcRequest.HEART_BEAT_REQUEST);
      else channel.close();
    }
    ReferenceCountUtil.release(evt);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
  }
}
