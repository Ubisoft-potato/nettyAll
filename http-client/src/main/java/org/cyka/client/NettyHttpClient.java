package org.cyka.client;

import com.google.common.base.Strings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.cyka.pool.ClientConnectionPool;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NettyHttpClient {

  private static final Bootstrap bootstrap = new Bootstrap();
  private static final EventLoopGroup GROUP = new NioEventLoopGroup();
  private static ChannelPoolMap<InetSocketAddress, ChannelPool> channelPoolMap;

  private InetSocketAddress targetServer;

  private void init() {
    bootstrap.channel(NioSocketChannel.class).group(GROUP);
    channelPoolMap = new ClientConnectionPool(bootstrap);
  }

  public NettyHttpClient connect(String host, Integer port) {
    checkArgument(!Strings.isNullOrEmpty(host), "host must not be null or empty!");
    checkArgument(port > 0 && port < 65535, "invalid port value!");
    this.targetServer = new InetSocketAddress(host, port);
    return this;
  }

  public void sendMsg(String uri) {
    ChannelPool pool = channelPoolMap.get(targetServer);
    try {
      Future<Channel> future = pool.acquire().sync();
      if (future.isSuccess()) {
        Channel channel = future.getNow();
        log.info("channel id : {}", channel.id());
        DefaultFullHttpRequest request =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        channel.writeAndFlush(request);
        pool.release(channel);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public NettyHttpClient() {
    init();
  }
}
