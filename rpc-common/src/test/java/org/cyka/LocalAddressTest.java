package org.cyka;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class LocalAddressTest {

  private final String serviceName = "testService";
  private final ServiceRegistry serviceRegistry = new EtcdServiceRegistry();

  @Test
  public void getLocalAddress() throws UnknownHostException {
    System.out.println(InetAddress.getLocalHost().getHostAddress());
  }

  @Test
  public void serviceRegisterAndGetTest() throws InterruptedException {
    serviceRegistry.watchServicesChange(Lists.newArrayList(serviceName));
    serviceRegistry.register(serviceName, 80);
    Thread.sleep(3000);
    log.info("service endpoint list: {}", serviceRegistry.getServiceEndpoints(serviceName));
    Thread.sleep(3000);
    serviceRegistry.disconnect();
  }

  @Test
  public void serviceWatchTest() {
    serviceRegistry.watchServicesChange(Lists.newArrayList(serviceName));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> System.exit(1)));
    LockSupport.park();
  }
}
