package org.cyka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import org.cyka.di.EtcdModule;
import org.cyka.registry.ServiceRegistry;
import org.junit.Test;

@Slf4j
public class RpcModuleTest {

  @Test
  public void etcdRegistry() {
    Injector injector = Guice.createInjector(new EtcdModule());
    Provider<ServiceRegistry> registryProvider = injector.getProvider(Key.get(ServiceRegistry.class, EtcdModule.etcd.class));
    registryProvider.get();
  }
}
