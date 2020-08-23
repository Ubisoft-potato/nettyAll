package org.cyka.client;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;
import org.cyka.pool.ClientConnectionPool;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NettyHttpClient {

  private static final Bootstrap bootstrap = new Bootstrap();
  private static final EventLoopGroup GROUP = new DefaultEventLoopGroup();
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

  public void sendStringMsg(String msg) throws InterruptedException {
    ChannelPool pool = channelPoolMap.get(targetServer);
    pool.acquire()
        .sync()
        .addListener(
            (FutureListener<Channel>)
                future -> {
                  if (future.isSuccess()) {
                    Channel channel = future.getNow();
//                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,new URI());
                    channel.writeAndFlush(Unpooled.copiedBuffer(msg, Charsets.UTF_8));
                    pool.release(channel);
                  }
                });
  }

  public NettyHttpClient() {
    init();
  }
}
