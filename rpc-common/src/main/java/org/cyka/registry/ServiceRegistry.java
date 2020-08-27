package org.cyka.registry;

import org.cyka.registry.lb.LoadBalanceStrategy;

import java.util.List;

public interface ServiceRegistry {

  /**
   * register a service to etcd with user define ipAddress
   *
   * @param serviceName
   * @param ipAddress
   * @param port
   */
  void register(String serviceName, String ipAddress, Integer port);

  /**
   * register a service to etcd , using localAddress
   *
   * @param serviceName
   * @param port
   */
  void register(String serviceName, Integer port);

  /**
   * get all service available endpoint
   *
   * @param serviceName
   * @return
   */
  List<ServiceEndpoint> getServiceEndpoints(String serviceName);

  /**
   * choose a service endpoint by specific lb strategy
   *
   * @param serviceName
   * @return
   */
  ServiceEndpoint choose(String serviceName, LoadBalanceStrategy strategy);

  /** when stop supply service , call this method to make current service offline */
  void disconnect();
}
