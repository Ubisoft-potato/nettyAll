package org.cyka.registry.lb;

import org.cyka.registry.ServiceEndpoint;

import java.util.List;

public abstract class LoadBalanceStrategy {
  private List<ServiceEndpoint> serviceEndpoints;

  public ServiceEndpoint choose() {
    return null;
  }

  public void updateServiceEndpoints(List<ServiceEndpoint> serviceEndpoints) {
    this.serviceEndpoints = serviceEndpoints;
  }

  public LoadBalanceStrategy(List<ServiceEndpoint> serviceEndpoints) {
    this.serviceEndpoints = serviceEndpoints;
  }
}
