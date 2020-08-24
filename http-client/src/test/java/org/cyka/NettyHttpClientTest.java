package org.cyka;

import org.cyka.client.NettyHttpClient;
import org.junit.Test;

public class NettyHttpClientTest {

  @Test
  public void SampleClientTest() {
    new NettyHttpClient().connect("localhost", 8080).sendMsg("/hello");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
