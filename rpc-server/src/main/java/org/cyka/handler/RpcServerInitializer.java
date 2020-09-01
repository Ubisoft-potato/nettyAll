package org.cyka.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.cyka.codec.RpcDecoder;
import org.cyka.codec.RpcEncoder;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.cyka.serializer.RpcSerializer;
import org.cyka.serializer.kryo.KryoSerializer;
import org.cyka.server.ServiceBeanRegistry;

import java.util.concurrent.TimeUnit;

public class RpcServerInitializer extends ChannelInitializer<Channel> {
  private final long allIdleTime;
  private final ServiceBeanRegistry beanRegistry;
  private final RpcSerializer serializer = new KryoSerializer();

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ch.pipeline()
        .addLast(new IdleStateHandler(0, 0, allIdleTime, TimeUnit.SECONDS))
        .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
        .addLast(new RpcDecoder(serializer, RpcRequest.class))
        .addLast(new RpcEncoder(serializer, RpcResponse.class))
        .addLast(new RpcServerHandler(beanRegistry));
  }

  public RpcServerInitializer(long allIdleTime, ServiceBeanRegistry beanRegistry) {
    this.allIdleTime = allIdleTime;
    this.beanRegistry = beanRegistry;
  }
}
