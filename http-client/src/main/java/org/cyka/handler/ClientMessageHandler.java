package org.cyka.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientMessageHandler extends SimpleChannelInboundHandler<HttpObject> {
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {}
}
