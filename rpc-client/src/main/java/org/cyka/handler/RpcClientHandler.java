package org.cyka.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;

@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse)
      throws Exception {}

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      log.debug(
          "channel: {} --> remote : {}, is reach idleTime, send heart beat request",
          ctx.channel().remoteAddress(),
          ctx.channel().id());
      ctx.writeAndFlush(RpcRequest.HEART_BEAT_REQUEST);
    }
    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
  }
}
