package org.cyka.registry.lb;

import org.cyka.registry.ServiceEndpoint;

import javax.management.ServiceNotFoundException;
import java.util.Set;

public abstract class LoadBalanceStrategy {

  protected volatile Set<ServiceEndpoint> serviceEndpoints;

  /**
   * choose a service by the template method <code>doLoadBalance</code>, sub class need to implement
   *
   * @param serviceName the service name to get a loadBalance endpoint
   * @param serviceEndpoints service endpoints to be choose
   * @return service endpoint
   * @throws ServiceNotFoundException service not found when no endpoint founded for the serviceName
   */
  public ServiceEndpoint choose(String serviceName, Set<ServiceEndpoint> serviceEndpoints)
      throws ServiceNotFoundException {
    this.serviceEndpoints = serviceEndpoints;
    return doLoadBalance(serviceName);
  }

  abstract ServiceEndpoint doLoadBalance(String serviceName) throws ServiceNotFoundException;
}
