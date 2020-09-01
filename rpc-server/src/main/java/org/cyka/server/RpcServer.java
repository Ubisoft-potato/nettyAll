package org.cyka.server;

import com.google.common.base.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.cyka.handler.RpcServerInitializer;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class RpcServer {
  private final ServerBootstrap serverBootstrap = new ServerBootstrap();
  private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
  private final EventLoopGroup workerGroup;
  private final ServiceRegistry serviceRegistry;
  private final ServiceBeanRegistry beanRegistry;
  private final String serverAddress;
  private final int serverPort;

  public void start() {
    try {
      serverBootstrap.bind(serverAddress, serverPort).sync();
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    stop();
                    System.exit(0);
                  },
                  "server-killer"));
    } catch (InterruptedException e) {
      log.error("Server Start Process has been Interrupted, reason: {}", e.getMessage());
    }
    // register service
    beanRegistry
        .getAllRegisteredServiceName()
        .forEach(
            serviceName -> {
              serviceRegistry.register(serviceName, serverAddress, serverPort);
              log.debug("service : {} been registered", serviceName);
            });
    log.info(
        "Rpc Server Started, listen Address: {}:{} , use control + c to stop the server",
        serverAddress,
        serverPort);
  }

  private void stop() {
    log.info("Rpc Server Shutting Down........");
    serviceRegistry.disconnect();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  private RpcServer(Builder builder) {
    checkArgument(builder.serverPort > 0, "please specify the server's port");
    checkArgument(
        !Strings.isNullOrEmpty(builder.basePackage),
        "serviceBean basePackage cannot be null or empty");
    this.serverPort = builder.serverPort;
    this.serverAddress =
        Strings.isNullOrEmpty(builder.serverAddress) ? "127.0.0.1" : builder.serverAddress;
    this.beanRegistry = new ServiceBeanRegistry(builder.basePackage);
    this.workerGroup =
        new NioEventLoopGroup(
            builder.nThread != 0 ? builder.nThread : Runtime.getRuntime().availableProcessors());
    this.serverBootstrap
        .channel(NioServerSocketChannel.class)
        .group(bossGroup, workerGroup)
        .childHandler(new RpcServerInitializer(60L, this.beanRegistry))
        .option(ChannelOption.SO_BACKLOG, 128);
    if (builder.debugEnabled) {
      this.serverBootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
    }
    // TODO: 2020/9/1 make a service registry option
    this.serviceRegistry = new EtcdServiceRegistry(builder.registryAddress);
  }

  public static class Builder {
    private String registryAddress;
    private int nThread;
    private boolean debugEnabled;
    private String serverAddress;
    private int serverPort;
    private String basePackage;

    public Builder nThread(int nThread) {
      this.nThread = nThread;
      return this;
    }

    public Builder registryAddresses(String registryAddress) {
      this.registryAddress = registryAddress;
      return this;
    }

    public Builder debugEnabled(boolean debugEnabled) {
      this.debugEnabled = debugEnabled;
      return this;
    }

    public Builder serverAddress(String serverAddress) {
      this.serverAddress = serverAddress;
      return this;
    }

    public Builder serverPort(int serverPort) {
      this.serverPort = serverPort;
      return this;
    }

    public Builder basePackage(String basePackage) {
      this.basePackage = basePackage;
      return this;
    }

    public RpcServer build() {
      return new RpcServer(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
