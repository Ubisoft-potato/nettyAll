package org.cyka.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.cyka.serializer.RpcSerializer;

import java.util.List;

@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

  private final RpcSerializer serializer;
  private final Class<?> clazz;

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() < 4) {
      return;
    }
    in.markReaderIndex();
    int dataLength = in.readInt();
    if (in.readableBytes() < dataLength) {
      // RpcRequest 未被完全接收 重置已读下标
      in.resetReaderIndex();
      return;
    }
    byte[] decodeObject = new byte[dataLength];
    in.readBytes(decodeObject);
    Object data = serializer.deserialize(decodeObject, clazz);
    out.add(data);
  }

  public RpcDecoder(RpcSerializer serializer, Class<?> clazz) {
    this.serializer = serializer;
    this.clazz = clazz;
  }
}
