package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.RpcCaller;
import org.cyka.client.NettyRpcClient;
import org.junit.Test;

@Slf4j
public class ServiceProxyTest {

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
