package org.cyka.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.*;
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

  private final long DEFAULT_ALL_IDLE_TIME = 60L;

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

  private NettyBasedConnectionPool(Builder builder) {
    this(
        builder.b,
        builder.channelIdleTime,
        builder.poolSize,
        builder.acquireTimeOutMills,
        builder.maxPendingAcquires,
        builder.useLIFO,
        builder.checker);
  }

  /**
   * the constructor of netty based connection pool
   *
   * @param b bootstrap
   * @param channelIdleTime channel idle time ,if reach ,will send a heart beat package
   * @param poolSize pool's max size
   * @param acquireTimeOutMills the max time (in milliseconds) the caller to wait
   * @param maxPendingAcquires the number of pending acquires
   * @param useLIFO whether to use LIFO , else to use FIFO
   * @param checker the health checker, must implement <code>ChannelHealthChecker</code>
   */
  private NettyBasedConnectionPool(
      Bootstrap b,
      long channelIdleTime,
      int poolSize,
      int acquireTimeOutMills,
      int maxPendingAcquires,
      boolean useLIFO,
      ChannelHealthChecker checker) {
    checkNotNull(b);
    checkArgument(poolSize > 0);
    checkArgument(acquireTimeOutMills > 0);
    checkArgument(maxPendingAcquires > 0);
    this.poolMap =
        new AbstractChannelPoolMap<ServiceEndpoint, ChannelPool>() {
          @Override
          protected ChannelPool newPool(ServiceEndpoint serviceEndpoint) {
            log.debug("make a new connectionPool for service endpoint : {}", serviceEndpoint);
            return new FixedChannelPool(
                b.remoteAddress(
                    new InetSocketAddress(serviceEndpoint.getHost(), serviceEndpoint.getPort())),
                new RpcChannelInitializer(channelIdleTime),
                checker != null ? checker : ChannelHealthChecker.ACTIVE,
                FixedChannelPool.AcquireTimeoutAction.NEW,
                acquireTimeOutMills,
                poolSize,
                maxPendingAcquires,
                true,
                useLIFO);
          }
        };
  }

  public static class Builder {
    private Bootstrap b;
    private int poolSize;
    private int acquireTimeOutMills;
    private int maxPendingAcquires;
    private boolean useLIFO;
    private ChannelHealthChecker checker;
    private long channelIdleTime;

    public Builder bootstrap(Bootstrap b) {
      this.b = b;
      return this;
    }

    public Builder poolSize(int poolSize) {
      this.poolSize = poolSize;
      return this;
    }

    public Builder acquireTimeOutMills(int acquireTimeOutMills) {
      this.acquireTimeOutMills = acquireTimeOutMills;
      return this;
    }

    public Builder maxPendingAcquires(int maxPendingAcquires) {
      this.maxPendingAcquires = maxPendingAcquires;
      return this;
    }

    public Builder useLIFO(boolean useLIFO) {
      this.useLIFO = useLIFO;
      return this;
    }

    public Builder checker(ChannelHealthChecker checker) {
      this.checker = checker;
      return this;
    }

    public Builder channelIdleTime(long channelIdleTime) {
      this.channelIdleTime = channelIdleTime;
      return this;
    }

    public NettyBasedConnectionPool build() {
      return new NettyBasedConnectionPool(this);
    }
  }
}
