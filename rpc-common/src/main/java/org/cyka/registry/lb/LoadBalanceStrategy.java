package org.cyka.registry.lb;

import org.cyka.registry.ServiceEndpoint;

public abstract class LoadBalanceStrategy {

  public ServiceEndpoint choose() {
    return null;
  }

}
