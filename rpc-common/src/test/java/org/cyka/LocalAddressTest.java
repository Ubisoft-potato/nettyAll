package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.etcd.EtcdServiceRegistry;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class LocalAddressTest {

  private final String serviceName = "testService";

  @Test
  public void getLocalAddress() throws UnknownHostException {
    System.out.println(InetAddress.getLocalHost().getHostAddress());
  }

  @Test
  public void serviceRegisterAndGetTest() {
    ServiceRegistry serviceRegistry = new EtcdServiceRegistry();
    serviceRegistry.register(serviceName, 80);
    log.info("service endpoint list: {}", serviceRegistry.getServiceEndpoints(serviceName));
    serviceRegistry.disconnect();
  }
}
