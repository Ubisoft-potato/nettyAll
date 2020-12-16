package org.cyka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.extern.slf4j.Slf4j;
import org.cyka.di.EtcdModule;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceRegistry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
public class RpcModuleTest {

  private Injector injector;

  @Before
  public void init() {
    injector = Guice.createInjector(EtcdModule.builder().etcdLeaseTTL(90).build());
  }

  @Test
  @Ignore("need etcd , only for local test")
  public void etcdRegistry() {
    ServiceRegistry serviceRegistry =
        injector.getInstance(Key.get(ServiceRegistry.class, EtcdModule.etcd.class));
    DiscoveryClient discoveryClient =
        injector.getInstance(Key.get(DiscoveryClient.class, EtcdModule.etcd.class));
    serviceRegistry.register("test", 80);
    log.info(discoveryClient.getServiceEndpoints("test").toString());
  }
}
