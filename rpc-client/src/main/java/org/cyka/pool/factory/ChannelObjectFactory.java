package org.cyka.pool.factory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.cyka.registry.ServiceEndpoint;

@Slf4j
public class ChannelObjectFactory extends BaseKeyedPooledObjectFactory<ServiceEndpoint, Channel> {

  private final Bootstrap b;

  @Override
  public Channel create(ServiceEndpoint key) throws Exception {
    return b.connect(key.getHost(), key.getPort()).sync().channel();
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<>(channel);
  }

  public ChannelObjectFactory(Bootstrap b) {
    this.b = b;
  }
}
