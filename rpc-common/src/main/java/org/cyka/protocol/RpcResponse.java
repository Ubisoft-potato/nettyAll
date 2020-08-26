package org.cyka.protocol;

import lombok.Data;

@Data
public class RpcResponse {
  /** 响应ID */
  private String requestId;
  /** 错误信息 */
  private String error;
  /** 返回的结果 */
  private Object result;
}
