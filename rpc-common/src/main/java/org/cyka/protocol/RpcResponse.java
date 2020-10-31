package org.cyka.protocol;

import lombok.Data;

@Data
public class RpcResponse {
  /** the server response id */
  private String requestId;
  /** error message */
  private String error;
  /** result */
  private Object result;
}
