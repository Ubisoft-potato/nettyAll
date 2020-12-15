package org.cyka.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.cyka.pool.NettyBasedConnectionPool;
import org.cyka.pool.RpcClientConnectionPool;
import org.cyka.proxy.CglibServiceProxy;
import org.cyka.proxy.jdk.JdkServiceProxy;
import org.cyka.proxy.ServiceProxy;

import java.util.Collection;

@Slf4j
public class NettyRpcClient {
  private final Bootstrap bootstrap = new Bootstrap();
  private final RpcClientConnectionPool connectionPool;
  private final NioEventLoopGroup executors;

  // RpcCaller 标注类所在包名
  private final String basePackage;
  private final Collection<Class<?>> callerServiceClasses;
  private final ServiceProxy serviceProxy;

  /**
   * query service instance
   *
   * @param clazz the class to query
   * @param <T> generic type
   * @return the instance
   */
  public <T> T getServiceCallerInstance(Class<T> clazz) {
    return serviceProxy.getInstance(clazz);
  }

  /**
   * check if the service class is been instantiated
   *
   * @param clazz the class to query
   * @return boolean
   */
  public boolean isServiceCallerBeInstantiated(Class<?> clazz) {
    return serviceProxy.getInstance(clazz) != null;
  }

  public void stop() {
    log.info("shutting down the client...");
    this.executors.shutdownGracefully();
  }

  public NettyRpcClient(Builder builder) {
    this.basePackage = builder.basePackage;
    this.callerServiceClasses = builder.callerServiceClasses;
    this.executors =
        new NioEventLoopGroup(
            builder.nThread != 0 ? builder.nThread : Runtime.getRuntime().availableProcessors());
    this.bootstrap
        .group(this.executors)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true);
    this.connectionPool =
        new NettyBasedConnectionPool.Builder()
            .bootstrap(bootstrap)
            .channelIdleTime(60L)
            .poolSize(20)
            .useLIFO(true)
            .maxPendingAcquires(500)
            .acquireTimeOutMills(6 * 1000)
            .build();
    switch (builder.proxyType) {
      case JDK:
        this.serviceProxy = new JdkServiceProxy(this.connectionPool);
        break;
      case CGLIB:
        this.serviceProxy = new CglibServiceProxy();
        break;
      default:
        throw new IllegalArgumentException("proxy type unrecognized");
    }
    serviceProxy.servicePackageScan(this.basePackage).generateServiceProxy(callerServiceClasses);
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public static class Builder {
    private int nThread;
    private String basePackage;
    private Collection<Class<?>> callerServiceClasses;
    private final ProxyType proxyType;

    public Builder(ProxyType proxyType) {
      this.proxyType = proxyType;
    }

    public Builder nThread(int nThread) {
      this.nThread = nThread;
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

  enum ProxyType {
    CGLIB,
    JDK
  }

  public static Builder builder() {
    return new Builder(ProxyType.JDK);
  }
}
