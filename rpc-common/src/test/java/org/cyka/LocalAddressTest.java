package org.cyka;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;
import org.cyka.registry.lb.RoundRobinLoadBalanceStrategy;
import org.cyka.registry.lb.RpcLoadBalance;
import org.junit.Test;

import javax.management.ServiceNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class LocalAddressTest {

  private final String serviceName = "testService";
  private final ServiceRegistry serviceRegistry = new EtcdServiceRegistry();
  private final RpcLoadBalance loadBalance = new RpcLoadBalance(serviceRegistry);

  @Test
  public void getLocalAddress() throws UnknownHostException {
    System.out.println(InetAddress.getLocalHost().getHostAddress());
  }

  @Test
  public void serviceRegisterAndGetTest() throws InterruptedException {
    registerAndWatchServices();
    Thread.sleep(100);
    log.info("service endpoint list: {}", serviceRegistry.getServiceEndpoints(serviceName));
    serviceRegistry.disconnect();
  }

  @Test
  public void serviceWatchTest() {
    serviceRegistry.watchServicesChange(Lists.newArrayList(serviceName));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  serviceRegistry.disconnect();
                  System.exit(1);
                }));
    LockSupport.park();
  }

  @Test
  public void roundRobinLoadBalanceTest() throws InterruptedException {
    registerAndWatchServices();
    Thread.sleep(100);
    try {
      RoundRobinLoadBalanceStrategy robinLoadBalanceStrategy = new RoundRobinLoadBalanceStrategy();
      for (int i = 0; i < 10; i++) {
        log.info(
            "loadBalance service endpoint: {}",
            loadBalance.chooseService(serviceName, robinLoadBalanceStrategy));
      }
    } catch (ServiceNotFoundException e) {
      log.warn(e.getMessage());
    }
    serviceRegistry.disconnect();
  }

  private void registerAndWatchServices() {
    serviceRegistry.watchServicesChange(Lists.newArrayList(serviceName));
    serviceRegistry.register(serviceName, 80);
    serviceRegistry.register(serviceName, 81);
    serviceRegistry.register(serviceName, 82);
  }
}
