package com.cyka.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

@Slf4j
public class HttpUploadServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (msg instanceof HttpRequest) {
      final HttpRequest httpRequest = (HttpRequest) msg;
      log.debug("http request uri: {}", (httpRequest).uri());
    }
    if (msg instanceof HttpContent) {
      final HttpContent httpContent = (HttpContent) msg;
      log.info("http content length: {} ", (httpContent).content().readableBytes());
      log.info("http content refCnt: {} ", (httpContent).content().refCnt());
      if (msg instanceof LastHttpContent) {
        final LastHttpContent lastHttpContent = (LastHttpContent) msg;
        log.info("last length: {}", (lastHttpContent).content().readableBytes());
        final DefaultFullHttpResponse response =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeBytes("netty".getBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("error: {}", cause.getCause().getMessage());
    ctx.channel().close();
  }
}
