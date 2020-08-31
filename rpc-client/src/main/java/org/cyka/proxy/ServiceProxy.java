package org.cyka.proxy;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import java.util.Collection;

public interface ServiceProxy {

  // guava classToInstance Map to keep generated service proxy object
  ClassToInstanceMap<Object> serviceProxyMap = MutableClassToInstanceMap.create();

  /**
   * scan the given path and generate service proxy
   *
   * @param basePackage the package path to scan
   */
  ServiceProxy servicePackageScan(String basePackage);

  /**
   * generate the given class's service proxy
   *
   * @param callerServiceClasses the service class to be generate
   */
  ServiceProxy generateServiceProxy(Collection<Class<?>> callerServiceClasses);

  /**
   * query a instance by specify the class args
   *
   * @param Class the class to query
   * @param <T> class type
   * @return the instance
   */
  <T> T getInstance(Class<T> Class);
}
