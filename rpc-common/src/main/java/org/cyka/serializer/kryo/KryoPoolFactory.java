package org.cyka.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.cyka.protocol.RpcRequest;
import org.cyka.protocol.RpcResponse;
import org.objenesis.strategy.StdInstantiatorStrategy;

public class KryoPoolFactory {
  private static volatile KryoPoolFactory poolFactory = null;

  private final KryoFactory factory =
      new KryoFactory() {
        @Override
        public Kryo create() {
          Kryo kryo = new Kryo();
          kryo.setReferences(false);
          kryo.register(RpcRequest.class);
          kryo.register(RpcResponse.class);
          Kryo.DefaultInstantiatorStrategy strategy =
              (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
          strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
          return kryo;
        }
      };

  private final KryoPool pool = new KryoPool.Builder(factory).build();

  public static KryoPool getKryoPoolInstance() {
    if (poolFactory == null) {
      synchronized (KryoPoolFactory.class) {
        if (poolFactory == null) {
          poolFactory = new KryoPoolFactory();
        }
      }
    }
    return poolFactory.getPool();
  }

  public KryoPool getPool() {
    return pool;
  }

  private KryoPoolFactory() {}
}
