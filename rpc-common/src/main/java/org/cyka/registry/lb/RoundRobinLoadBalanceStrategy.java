package org.cyka.registry.lb;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceEndpoint;

import javax.management.ServiceNotFoundException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinLoadBalanceStrategy extends LoadBalanceStrategy {

  private final AtomicInteger serviceIndex = new AtomicInteger(0);

  @Override
  ServiceEndpoint doLoadBalance(String serviceName) throws ServiceNotFoundException {
    if (Objects.nonNull(serviceEndpoints) && serviceEndpoints.size() > 0) {
      if (serviceIndex.get() < serviceEndpoints.size()) {
        int currentIndex = serviceIndex.getAndIncrement();
        log.debug(
            "current service index : {}, next service index: {}", currentIndex, serviceIndex.get());
        return Iterators.get(serviceEndpoints.iterator(), currentIndex);
      } else {
        // return the index to 0 to make a round robin
        log.debug(
            "service index reach to the  max size: {}, reset the  service index to : 0 ",
            serviceIndex.get());
        // // TODO: 2020/8/28 update this index after reset
        return Iterators.get(serviceEndpoints.iterator(), serviceIndex.updateAndGet(operand -> 0));
      }
    }
    throw new ServiceNotFoundException(
        "cannot find available service endpoint for service: " + serviceName);
  }
}
