package org.cyka.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.cyka.codec.RpcDecoder;
import org.cyka.codec.RpcEncoder;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.cyka.serializer.RpcSerializer;
import org.cyka.serializer.kryo.KryoSerializer;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcChannelInitializer extends ChannelInitializer<Channel> {

  private final RpcSerializer serializer = new KryoSerializer();
  private final long allIdleTime;

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel
        .pipeline()
        .addLast(new IdleStateHandler(0, 0, allIdleTime, TimeUnit.SECONDS))
        // first 4 byte is the length of this msg
        .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
        .addLast(new RpcDecoder(serializer, RpcResponse.class))
        .addLast(new RpcEncoder(serializer, RpcRequest.class))
        .addLast(new RpcClientHandler());
  }

  public RpcChannelInitializer(long allIdleTime) {
    this.allIdleTime = allIdleTime;
  }
}
