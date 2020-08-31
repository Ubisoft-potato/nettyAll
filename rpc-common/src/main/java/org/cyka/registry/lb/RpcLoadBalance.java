package org.cyka.registry.lb;

import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceEndpoint;

import javax.management.ServiceNotFoundException;
import java.util.Set;

@Slf4j
public class RpcLoadBalance {

  private final DiscoveryClient discoveryClient;

  /**
   * choose a service Endpoint for specific serviceName using specific LoadBalanceStrategy
   *
   * @param serviceName the service name to get a loadBalance endpoint
   * @param strategy the loadBalance strategy
   * @return serviceEndpoint
   */
  public ServiceEndpoint chooseService(String serviceName, LoadBalanceStrategy strategy)
      throws ServiceNotFoundException {
    Set<ServiceEndpoint> serviceEndpoints = discoveryClient.getServiceEndpoints(serviceName);
    log.debug("service list to be loadBalance : {}", serviceEndpoints);
    return strategy.choose(serviceName, serviceEndpoints);
  }

  public RpcLoadBalance(DiscoveryClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }
}
