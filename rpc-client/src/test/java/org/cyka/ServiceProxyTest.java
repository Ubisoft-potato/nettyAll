package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.RpcCaller;
import org.cyka.proxy.JdkServiceProxy;
import org.cyka.proxy.ServiceProxy;
import org.junit.Test;

@Slf4j
public class ServiceProxyTest {

  @Test
  public void jdkProxyTest() {
    ServiceProxy serviceProxy = new JdkServiceProxy().servicePackageScan("org.cyka");
    log.info(
        "generated service object: {}", serviceProxy.getInstance(HelloService.class).toString());
  }

  @RpcCaller(serviceName = "helloService")
  static interface HelloService {}
}
