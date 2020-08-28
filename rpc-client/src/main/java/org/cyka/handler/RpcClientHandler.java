package org.cyka.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.cyka.protocol.RpcResponse;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse)
      throws Exception {}
}
