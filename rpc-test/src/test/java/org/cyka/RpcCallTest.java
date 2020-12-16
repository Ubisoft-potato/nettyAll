package org.cyka;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.cyka.client.NettyRpcClient;
import org.cyka.proxy.jdk.JdkServiceProxy;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.etcd.EtcdDiscoveryClient;
import org.cyka.registry.etcd.EtcdServiceRegistry;
import org.cyka.server.RpcServer;
import org.cyka.service.HelloService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyRpcClient.class, RpcServer.class, JdkServiceProxy.class})
public class RpcCallTest {

  private static final String BASE_PACKAGE = "org.cyka";

  RpcServer rpcServer;
  NettyRpcClient rpcClient;
  @Mock EtcdServiceRegistry etcdServiceRegistry;
  @Mock EtcdDiscoveryClient etcdDiscoveryClient;

  @Before
  public void setup() {
    try {
      whenNew(EtcdServiceRegistry.class).withAnyArguments().thenReturn(etcdServiceRegistry);
      whenNew(EtcdDiscoveryClient.class).withAnyArguments().thenReturn(etcdDiscoveryClient);
      doNothing().when(etcdDiscoveryClient).watchServicesChange(Sets.newHashSet(anyIterable()));
      when(etcdDiscoveryClient.getServiceEndpoints(anyString()))
          .thenReturn(Sets.newHashSet(new ServiceEndpoint("localhost", 8888)));
      doNothing().when(etcdDiscoveryClient).disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
    rpcServer =
        RpcServer.builder()
            .nThread(8)
            .debugEnabled(true)
            .serverPort(8888)
            .basePackage("org.cyka")
            .build();

    rpcClient = NettyRpcClient.builder().basePackage(BASE_PACKAGE).nThread(8).build();

    rpcServer.start();
  }

  @After
  public void cleanup() {
    rpcClient.stop();
    rpcServer.stop();
  }

  @Test
  public void helloServiceCallTest() throws InterruptedException {
    HelloService helloService = rpcClient.getServiceCallerInstance(HelloService.class);
    ExecutorService service = Executors.newFixedThreadPool(8);
    int callCount = 10;
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
