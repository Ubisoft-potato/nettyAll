package org.cyka.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import lombok.extern.slf4j.Slf4j;
import org.cyka.handler.ClientChannelInitializer;

import java.net.InetSocketAddress;

/** client channel pool, using netty built-in ChannelPool */
@Slf4j
public class ClientConnectionPool extends AbstractChannelPoolMap<InetSocketAddress, ChannelPool> {

  private final Bootstrap bootstrap;

  public ClientConnectionPool(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  protected ChannelPool newPool(InetSocketAddress key) {
    log.debug("create channel new pool for host: {}, port: {}", key.getHostName(), key.getPort());
    return new FixedChannelPool(bootstrap.remoteAddress(key), new ClientChannelInitializer(), 20);
  }
}
