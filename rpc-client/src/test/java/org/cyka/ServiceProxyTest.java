package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.RpcCaller;
import org.cyka.client.NettyRpcClient;
import org.cyka.proxy.JdkServiceProxy;
import org.cyka.proxy.ServiceProxy;
import org.junit.Test;

@Slf4j
public class ServiceProxyTest {

  @Test
  public void jdkProxyTest() {
    ServiceProxy serviceProxy = new JdkServiceProxy(null).servicePackageScan("org.cyka");
    log.info(
        "generated service object: {}", serviceProxy.getInstance(HelloService.class).toString());
  }

  @Test
  public void rpcCallTest() {
    NettyRpcClient rpcClient = NettyRpcClient.builder().basePackage("org.cyka").nThread(8).build();
    HelloService helloService = rpcClient.getServiceCallerInstance(HelloService.class);
    log.info(helloService.hello());
  }

  @RpcCaller(serviceName = "helloService")
  interface HelloService {
    String hello();
  }
}
