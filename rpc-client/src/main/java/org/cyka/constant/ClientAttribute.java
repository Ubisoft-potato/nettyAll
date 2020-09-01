package org.cyka.constant;

import io.netty.util.AttributeKey;
import org.cyka.async.AsyncResult;
import org.cyka.protocol.RpcResponse;

import java.util.concurrent.ConcurrentMap;

public class ClientAttribute {
  // map bind to channel , store  RpcResponse' callback
  public static final AttributeKey<ConcurrentMap<String, AsyncResult<RpcResponse>>>
      RESPONSE_CALLBACK_MAP = AttributeKey.newInstance("RESPONSE_CALLBACK_MAP");
}
