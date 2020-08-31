package org.cyka.registry;

import java.util.Set;

public interface ServiceRegistry {

  /**
   * register a service to etcd with user define ipAddress
   *
   * @param serviceName service name
   * @param ipAddress ip address
   * @param port service port
   */
  void register(String serviceName, String ipAddress, Integer port);

  /**
   * register a service to etcd , using localAddress
   *
   * @param serviceName service name
   * @param port service port
   */
  void register(String serviceName, Integer port);

  /**
   * get all service available endpoint
   *
   * @param serviceName
   * @return service endpoints
   */
  Set<ServiceEndpoint> getServiceEndpoints(String serviceName);

  /**
   * watch on service's change, for example: service offline and host change
   *
   * @param serviceNames services to be watch
   */
  void watchServicesChange(Iterable<String> serviceNames);

  /** when stop supply service , call this method to make current service offline */
  void disconnect();
}
