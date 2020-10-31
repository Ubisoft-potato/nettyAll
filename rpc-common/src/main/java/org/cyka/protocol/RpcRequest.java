package org.cyka.protocol;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class RpcRequest {
  /** request object id */
  private String requestId;
  /** rpc request class name */
  private String className;
  /** service version */
  private String version;
  /** the method name to call */
  private String methodName;
  /** parameter type */
  private Class<?>[] parameterTypes;
  /** parameters */
  private Object[] parameters;

  /** heart beat package */
  public static final RpcRequest HEART_BEAT_REQUEST =
      new RpcRequest().setRequestId("HEART_BEAT_REQUEST");
}
