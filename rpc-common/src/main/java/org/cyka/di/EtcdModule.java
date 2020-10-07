package org.cyka.di;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdClientHolder;
import org.cyka.registry.etcd.EtcdDiscoveryClient;
import org.cyka.registry.etcd.EtcdServiceRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Slf4j
@Builder
public class EtcdModule extends AbstractModule {

  private final String etcdRegistryAddress;
  private final int etcdLeaseTTL;

  @Override
  protected void configure() {
    bindConstant()
        .annotatedWith(Names.named("etcdRegistryAddress"))
        .to(
            Strings.isNullOrEmpty(etcdRegistryAddress)
                ? EtcdClientHolder.DEFAULT_ADDRESS
                : etcdRegistryAddress);
    bindConstant()
        .annotatedWith(Names.named("etcdLeaseTTL"))
        .to(etcdLeaseTTL == 0 ? EtcdClientHolder.ETCD_LEASE_TTL : etcdLeaseTTL);
  }

  @Provides
  @etcd
  ServiceRegistry etcdServiceRegistry(
      @Named("etcdRegistryAddress") String etcdRegistryAddress,
      @Named("etcdLeaseTTL") int etcdLeaseTTL) {
    return new EtcdServiceRegistry(etcdRegistryAddress, etcdLeaseTTL);
  }

  @Provides
  @etcd
  DiscoveryClient etcdDiscoveryClient(@Named("etcdRegistryAddress") String etcdRegistryAddress) {
    return new EtcdDiscoveryClient(etcdRegistryAddress);
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.FIELD})
  public @interface etcd {}
}
