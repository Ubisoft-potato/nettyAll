package org.cyka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.extern.slf4j.Slf4j;
import org.cyka.di.EtcdModule;
import org.cyka.registry.ServiceRegistry;
import org.junit.Test;

@Slf4j
public class RpcModuleTest {

  @Test
  public void etcdRegistry() {
    Injector injector = Guice.createInjector(new EtcdModule());
    ServiceRegistry serviceRegistry =
        injector.getInstance(Key.get(ServiceRegistry.class, EtcdModule.etcd.class));
    serviceRegistry.register("test", 80);
  }
}
