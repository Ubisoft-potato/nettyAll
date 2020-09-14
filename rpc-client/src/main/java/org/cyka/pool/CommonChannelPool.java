package org.cyka.pool;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.cyka.async.AsyncCallback;
import org.cyka.async.AsyncResult;
import org.cyka.registry.ServiceEndpoint;

@Slf4j
public class CommonChannelPool implements RpcClientConnectionPool {

  private final GenericKeyedObjectPool<ServiceEndpoint, Channel> channelObjectPool;

  @Override
  public AsyncResult<Channel> acquireChannel(ServiceEndpoint serviceEndpoint) {
    try {
      // todo async borrow channel
      channelObjectPool.borrowObject(serviceEndpoint);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public AsyncResult<Channel> acquireChannel(
      ServiceEndpoint serviceEndpoint, AsyncCallback<Channel> callback) {
    return null;
  }

  @Override
  public AsyncResult<Void> releaseChannel(
      ServiceEndpoint serviceEndpoint, Channel channel, AsyncCallback<Void> callback) {
    return null;
  }

  @Override
  public void close() {
    channelObjectPool.close();
  }

  public CommonChannelPool(KeyedPooledObjectFactory<ServiceEndpoint, Channel> objectFactory) {
    GenericKeyedObjectPoolConfig<Channel> config = new GenericKeyedObjectPoolConfig<>();
    config.setMaxTotal(0);
    config.setMaxTotalPerKey(0);
    config.setMinIdlePerKey(0);
    config.setMaxIdlePerKey(0);
    config.setLifo(false);
    config.setFairness(false);
    config.setMaxWaitMillis(0L);
    config.setMinEvictableIdleTimeMillis(0L);
    config.setSoftMinEvictableIdleTimeMillis(0L);
    config.setNumTestsPerEvictionRun(0);
    config.setEvictorShutdownTimeoutMillis(0L);
    config.setTestOnCreate(true);
    config.setTestOnBorrow(true);
    config.setTestOnReturn(true);
    config.setTestWhileIdle(true);
    config.setTimeBetweenEvictionRunsMillis(0L);
    config.setEvictionPolicy(new DefaultEvictionPolicy<>());
    config.setEvictionPolicyClassName("");
    config.setBlockWhenExhausted(false);
    config.setJmxEnabled(false);
    config.setJmxNameBase("");
    config.setJmxNamePrefix("");
    this.channelObjectPool = new GenericKeyedObjectPool<>(objectFactory, config);
  }
}
