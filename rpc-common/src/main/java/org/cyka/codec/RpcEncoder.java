package org.cyka.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.cyka.protocol.RpcRequest;
import org.cyka.serializer.RpcSerializer;

@Slf4j
public class RpcEncoder extends MessageToByteEncoder<RpcRequest> {

  private final RpcSerializer serializer;

  @Override
  protected void encode(ChannelHandlerContext ctx, RpcRequest msg, ByteBuf out) throws Exception {
    byte[] requestData = serializer.serialize(msg);
    out.writeInt(requestData.length);  // 不定长数据包
    out.writeBytes(requestData);
  }

  public RpcEncoder(RpcSerializer serializer) {
    this.serializer = serializer;
  }
}
