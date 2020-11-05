package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.client.NettyRpcClient;
import org.cyka.service.HelloService;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RpcCallTest {

  @Test
  public void helloServiceCallTest() throws InterruptedException {
    NettyRpcClient rpcClient = NettyRpcClient.builder().basePackage("org.cyka").nThread(8).build();
    HelloService helloService = rpcClient.getServiceCallerInstance(HelloService.class);
    ExecutorService service = Executors.newFixedThreadPool(8);
    int callCount = 100000;
    CountDownLatch latch = new CountDownLatch(callCount);
    for (int i = 0; i < callCount; i++) {
      service.execute(
          () -> {
            log.info(helloService.sayHello("Rpc"));
            log.info(helloService.repeat("Rpc").toString());
            latch.countDown();
          });
    }
    latch.await();
    service.shutdown();
  }
}
