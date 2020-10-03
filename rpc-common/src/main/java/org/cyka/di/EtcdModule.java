package org.cyka.di;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdClientHolder;
import org.cyka.registry.etcd.EtcdServiceRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Slf4j
public class EtcdModule extends AbstractModule {

  @Override
  protected void configure() {
    bindConstant()
        .annotatedWith(Names.named("etcdRegistryAddress"))
        .to(EtcdClientHolder.DEFAULT_ADDRESS);
    bindConstant().annotatedWith(Names.named("etcdLeaseTTL")).to(60);
  }

  @Provides
  @etcd
  ServiceRegistry etcdServiceRegistry(
      @Named("etcdRegistryAddress") String etcdRegistryAddress,
      @Named("etcdLeaseTTL") int etcdLeaseTTL) {
    return new EtcdServiceRegistry(etcdRegistryAddress, etcdLeaseTTL);
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.FIELD})
  public @interface etcd {}
}
