package org.cyka.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.cyka.pool.RpcClientConnectionPool;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class NettyRpcClient {
  private final Bootstrap bootstrap = new Bootstrap();
  private final RpcClientConnectionPool connectionPool;

  // RpcCaller 标注类所在包名
  private final String basePackage;
  private final Collection<Class<?>> callerServiceClasses;




  /**
   * add service class to generate
   *
   * @param clazz the service class to be add
   */
  public void addCallerServiceClass(Class<?> clazz) {
    callerServiceClasses.add(clazz);
  }

  public Collection<Class<?>> getCallerServiceClasses() {
    return callerServiceClasses;
  }

  public NettyRpcClient(Builder builder) {
    checkNotNull(builder.pool);
    this.connectionPool = builder.pool;
    this.basePackage = builder.basePackage;
    this.callerServiceClasses = builder.callerServiceClasses;
    this.bootstrap
        .group(
            new NioEventLoopGroup(
                builder.nThread != 0
                    ? builder.nThread
                    : Runtime.getRuntime().availableProcessors()))
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true);
  }

  public static class Builder {
    private int nThread;
    private RpcClientConnectionPool pool;
    private String basePackage;
    private Collection<Class<?>> callerServiceClasses;

    public Builder nThread(int nThread) {
      this.nThread = nThread;
      return this;
    }

    public Builder pool(RpcClientConnectionPool pool) {
      this.pool = pool;
      return this;
    }

    public Builder basePackage(String basePackage) {
      this.basePackage = basePackage;
      return this;
    }

    public Builder callerServiceClasses(Collection<Class<?>> callerServiceClasses) {
      this.callerServiceClasses = callerServiceClasses;
      return this;
    }

    public NettyRpcClient build() {
      return new NettyRpcClient(this);
    }
  }
}
