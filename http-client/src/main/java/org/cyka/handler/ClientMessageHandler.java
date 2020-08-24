package org.cyka.handler;

import com.google.common.base.Charsets;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientMessageHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
    log.info(msg.content().toString(Charsets.UTF_8));
  }
}
