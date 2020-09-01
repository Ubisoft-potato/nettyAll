package org.cyka;

import org.cyka.server.RpcServer;

public class RpcApplication {
  public static void main(String[] args) {
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
