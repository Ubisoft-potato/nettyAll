package org.cyka;

import org.cyka.server.RpcServer;
import org.junit.Test;

public class ServerTest {

  @Test
  public void serverBootTest() throws InterruptedException {
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
