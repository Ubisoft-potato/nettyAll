package org.cyka.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;
import org.cyka.async.AsyncCallback;
import org.cyka.async.AsyncResult;
import org.cyka.async.CompletableResult;
import org.cyka.handler.RpcChannelInitializer;
import org.cyka.registry.ServiceEndpoint;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class NettyBasedConnectionPool implements RpcClientConnectionPool {

  private final ChannelPoolMap<ServiceEndpoint, ChannelPool> poolMap;

  @Override
  public AsyncResult<Channel> acquireChannel(ServiceEndpoint serviceEndpoint) {
    return acquireChannel(serviceEndpoint, null);
  }

  @Override
  public AsyncResult<Channel> acquireChannel(
      ServiceEndpoint serviceEndpoint, AsyncCallback<Channel> callback) {
    CompletableResult<Channel> channelResult = new CompletableResult<>(callback);
    poolMap
        .get(serviceEndpoint)
        .acquire()
        .addListener(
            (FutureListener<Channel>)
                future -> {
                  if (future.isSuccess()) {
                    Channel acquiredChannel = future.getNow();
                    if (acquiredChannel.isActive()) {
                      channelResult.setValue(acquiredChannel);
                    } else {
                      channelResult.setException(
                          new RuntimeException(
                              "the acquired channel : " + acquiredChannel.id() + "is not active"));
                    }
                  } else {
                    channelResult.setException(
                        new RuntimeException(
                            "cannot get connection for service endpoint: "
                                + serviceEndpoint
                                + " ,reason: "
                                + future.cause().getLocalizedMessage()));
                  }
                });
    return channelResult;
  }

  @Override
  public AsyncResult<Void> releaseChannel(
      ServiceEndpoint serviceEndpoint, Channel channel, AsyncCallback<Void> callback) {
    CompletableResult<Void> result = new CompletableResult<>(callback);
    poolMap
        .get(serviceEndpoint)
        .release(channel)
        .addListener(
            (FutureListener<Void>)
                future -> {
                  if (future.isSuccess()) {
                    result.setValue(Void.TYPE.newInstance());
                  } else {
                    result.setException(
                        new RuntimeException(
                            "release channel: "
                                + channel.id()
                                + " failed ,reason:"
                                + future.cause().getLocalizedMessage()));
                  }
                });
    return result;
  }

  public NettyBasedConnectionPool(Bootstrap b, int poolSize) {
    checkNotNull(b);
    checkArgument(poolSize > 0);
    this.poolMap =
        new AbstractChannelPoolMap<ServiceEndpoint, ChannelPool>() {
          @Override
          protected ChannelPool newPool(ServiceEndpoint serviceEndpoint) {
            log.debug("make a new connectionPool for service endpoint : {}", serviceEndpoint);
            InetSocketAddress remoteAddress =
                new InetSocketAddress(serviceEndpoint.getHost(), serviceEndpoint.getPort());
            return new FixedChannelPool(
                b.remoteAddress(remoteAddress), new RpcChannelInitializer(60L), poolSize);
          }
        };
  }
}
