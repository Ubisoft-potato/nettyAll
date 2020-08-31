package org.cyka.registry.etcd;

import io.etcd.jetcd.Client;

public class EtcdClientHolder {
  private static volatile Client client;
  // 注册中心地址
  private static final String DEFAULT_ADDRESS = "http://127.0.0.1:2379";
  private static final Object lock = new Object();

  public static Client createClient(String registryAddress) {
    if (client == null) {
      synchronized (lock) {
        if (client != null) {
          return client;
        }
        registryAddress = registryAddress != null ? registryAddress : DEFAULT_ADDRESS;
        // build etcd client
        client = Client.builder().endpoints(registryAddress).build();
      }
      return client;
    }
    return client;
  }

  public static void disconnect() {
    if (client != null) {
      client.close();
    }
  }
}