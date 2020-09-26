package com.cyka;

import com.cyka.handler.HttpServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

/**
 * @author long
 * @version 1.0.0
 * @since 2020/9/26 17:30
 */
@Slf4j
public class HttpFileServer {
  private final ServerBootstrap sb = new ServerBootstrap();
  private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
  private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
  private boolean isDebugEnable = false;
  private int serverPort = 8848;

  public void start() {
    sb.channel(NioServerSocketChannel.class)
        .group(bossGroup, workerGroup)
        .childHandler(new HttpServerInitializer());
    if (isDebugEnable) {
      sb.handler(new LoggingHandler(LogLevel.DEBUG));
    }
    SocketAddress localAddress = sb.bind(serverPort).syncUninterruptibly().channel().localAddress();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("http file server shutting down");
                  bossGroup.shutdownGracefully();
                  workerGroup.shutdownGracefully();
                },
                "http-server-killer"));
    log.info("http file server is running at: {}", localAddress);
  }

  public static void main(String[] args) {
    new HttpFileServer(true).start();
  }

  public HttpFileServer() {}

  public HttpFileServer(boolean isDebugEnable, int serverPort) {
    this.isDebugEnable = isDebugEnable;
    this.serverPort = serverPort;
  }

  public HttpFileServer(boolean isDebugEnable) {
    this.isDebugEnable = isDebugEnable;
  }

  public HttpFileServer(int serverPort) {
    this.serverPort = serverPort;
  }
}
