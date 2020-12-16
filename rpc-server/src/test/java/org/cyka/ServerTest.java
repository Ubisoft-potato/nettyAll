package org.cyka;

import org.cyka.registry.etcd.EtcdServiceRegistry;
import org.cyka.server.RpcServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RpcServer.class)
public class ServerTest {

  @Mock EtcdServiceRegistry dummyEtcdServiceRegistry;

  @Test
  public void serverBootWithEtcdTest() {
    try {
      whenNew(EtcdServiceRegistry.class).withAnyArguments().thenReturn(dummyEtcdServiceRegistry);
    } catch (Exception e) {
      e.printStackTrace();
    }
    RpcServer rpcServer =
        RpcServer.builder()
            .nThread(8)
            .debugEnabled(true)
            .serverPort(8888)
            .basePackage("org.cyka")
            .build();
    rpcServer.start();
  }
}
