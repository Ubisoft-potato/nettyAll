package org.cyka.serializer;

public interface RpcSerializer {
  /**
   * object ---> byte array
   *
   * @param obj  the object to be serialized
   * @param <T> the object type
   * @return byte array
   */
  <T> byte[] serialize(T obj);

  /**
   * byte array---> object
   *
   * @param bytes byte array
   * @param <T> the object type
   * @return deserialized object
   */
  <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
