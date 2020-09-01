package org.cyka.registry;

import java.util.Set;

public interface DiscoveryClient {
  /**
   * get all service available endpoint
   *
   * @param serviceName the serVice name to query
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
