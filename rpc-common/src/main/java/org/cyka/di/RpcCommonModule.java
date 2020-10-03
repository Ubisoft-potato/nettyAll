package org.cyka.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;

/** Class -> instance bind */
public class RpcCommonModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  ServiceRegistry serviceRegistry(String a) {
    return new EtcdServiceRegistry();
  }
}
