package org.cyka.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import lombok.extern.slf4j.Slf4j;
import org.cyka.serializer.RpcSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class KryoSerializer implements RpcSerializer {

  // kryo 对象为非线程安全，池化处理
  private final KryoPool pool = KryoPoolFactory.getKryoPoolInstance();

  public <T> byte[] serialize(T obj) {
    Kryo kryo = pool.borrow();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Output out = new Output(byteArrayOutputStream);
    try {
      kryo.writeObject(out, obj);
      out.close();
      return byteArrayOutputStream.toByteArray();
    } catch (KryoException ex) {
      throw new RuntimeException(ex);
    } finally {
      try {
        byteArrayOutputStream.close();
      } catch (IOException e) {
        log.warn("OutputArrayBuffer Close Error");
      }
      pool.release(kryo);
    }
  }

  public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
    Kryo kryo = pool.borrow();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    Input in = new Input(byteArrayInputStream);
    try {
      Object result = kryo.readObject(in, clazz);
      in.close();
      return result;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      try {
        byteArrayInputStream.close();
      } catch (IOException e) {
        log.warn("InputArrayBuffer Close Error");
      }
      pool.release(kryo);
    }
  }
}
