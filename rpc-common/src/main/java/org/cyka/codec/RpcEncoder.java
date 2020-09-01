package org.cyka.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.cyka.serializer.RpcSerializer;

@Slf4j
public class RpcEncoder extends MessageToByteEncoder<Object> {

  private final RpcSerializer serializer;
  private final Class<?> clazz;

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    if (clazz.isInstance(msg)) {
      byte[] sendData = serializer.serialize(msg);
      out.writeInt(sendData.length); // 不定长数据包
      out.writeBytes(sendData);
    }
  }

  public RpcEncoder(RpcSerializer serializer, Class<?> clazz) {
    this.serializer = serializer;
    this.clazz = clazz;
  }
}
