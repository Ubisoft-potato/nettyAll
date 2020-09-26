package com.cyka.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author long
 * @version 1.0.0
 * @since 2020/9/26 17:31
 */
@Slf4j
public class HttpServerInitializer extends ChannelInitializer<Channel> {
  private final int CHANNEL_MAX_BUFFER_SIZE = 10 * 1024 * 1024;

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ch.config().setWriteBufferHighWaterMark(CHANNEL_MAX_BUFFER_SIZE);
    ch.pipeline()
        .addLast(new HttpServerCodec())
        .addLast(new ChunkedWriteHandler())
        .addLast(new HttpUploadServerHandler());
  }
}
