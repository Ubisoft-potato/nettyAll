package org.cyka.registry.lb;

import org.cyka.registry.ServiceEndpoint;

import javax.management.ServiceNotFoundException;

/**
 * consistence hash implement
 *
 * @author long
 * @version 1.0.0
 * @since 2020/12/7 22:03
 */
public class ConsistentHashStrategy extends LoadBalanceStrategy {
  @Override
  ServiceEndpoint doLoadBalance(String serviceName) throws ServiceNotFoundException {
    return null;
  }
}
