package org.cyka.handler;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientChannelInitializer implements ChannelPoolHandler {

  public void channelReleased(Channel ch) throws Exception {
    log.info("channel: {}  release to the pool...", ch.id());
  }

  public void channelAcquired(Channel ch) throws Exception {
    log.info("channel: {} been acquired", ch.id());
  }

  public void channelCreated(Channel ch) throws Exception {
    log.info("new channel : {} , created", ch.id());
    SocketChannel channel = (SocketChannel) ch;
    channel.config().setKeepAlive(true);
    channel.config().setTcpNoDelay(true);
    ch.pipeline()
        .addLast(new IdleStateHandler(0, 0, 5, TimeUnit.SECONDS))
        .addLast(new LoggingHandler(LogLevel.DEBUG))
        .addLast("httpClientCodec", new HttpClientCodec())
        .addLast(new ChunkedWriteHandler())
        .addLast(new ClientMessageHandler());
  }
}
