package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.client.NettyRpcClient;
import org.cyka.service.HelloService;
import org.junit.Test;

@Slf4j
public class RpcCallTest {

  @Test
  public void helloServiceCallTest() {
    NettyRpcClient rpcClient = NettyRpcClient.builder().basePackage("org.cyka").nThread(8).build();
    HelloService helloService = rpcClient.getServiceCallerInstance(HelloService.class);
    log.info(helloService.sayHello("Rpc"));
    log.info(helloService.repeat("Rpc").toString());
  }
}
