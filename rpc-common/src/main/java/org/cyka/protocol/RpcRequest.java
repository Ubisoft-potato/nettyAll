package org.cyka.protocol;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class RpcRequest {
  /** 请求对象的ID */
  private String requestId;
  /** 类名 */
  private String className;
  /** 方法名 */
  private String methodName;
  /** 参数类型 */
  private Class<?>[] parameterTypes;
  /** 入参 */
  private Object[] parameters;

  /** heart beat package */
  public static final RpcRequest HEART_BEAT_REQUEST =
      new RpcRequest().setRequestId("HEART_BEAT_REQUEST");
}
