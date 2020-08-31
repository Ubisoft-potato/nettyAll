package org.cyka.proxy;

import java.util.Collection;

public class CglibServiceProxy implements ServiceProxy {
  @Override
  public ServiceProxy servicePackageScan(String basePackage) {
    return this;
  }

  @Override
  public ServiceProxy generateServiceProxy(Collection<Class<?>> callerServiceClasses) {
    return this;
  }

  @Override
  public <T> T getInstance(Class<T> Class) {
    return null;
  }
}
