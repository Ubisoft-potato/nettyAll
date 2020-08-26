package org.cyka.serializer;

public interface RpcSerializer {
  /**
   * object ---> byte array
   *
   * @param obj
   * @param <T>
   * @return
   */
  <T> byte[] serialize(T obj);

  /**
   * byte array---> object
   *
   * @param bytes
   * @param <T>
   * @return
   */
  <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
