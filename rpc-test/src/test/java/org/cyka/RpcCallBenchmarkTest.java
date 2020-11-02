package org.cyka;

import org.cyka.client.NettyRpcClient;
import org.cyka.server.RpcServer;
import org.cyka.service.HelloService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author long
 * @version 1.0.0
 * @since 2020/11/2 11:16
 */
@State(Scope.Benchmark)
public class RpcCallBenchmarkTest {
  private RpcServer server;
  private NettyRpcClient client;
  private HelloService helloService;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder().include(RpcCallBenchmarkTest.class.getSimpleName()).forks(1).build();

    new Runner(opt).run();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(iterations = 2, time = 2)
  @Measurement(iterations = 2)
  @Threads(8)
  @Fork(
      value = 2,
      jvmArgs = {"-Xms2G", "-Xmx2G"})
  public void rpcCall(Blackhole bh) {
    String rpcMsg = helloService.sayHello("rpc msg");
    bh.consume(rpcMsg);
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  @Warmup(iterations = 2, time = 2)
  @Measurement(iterations = 2)
  @Threads(8)
  @Fork(
      value = 2,
      jvmArgs = {"-Xms2G", "-Xmx2G"})
  public void rpcListCall(Blackhole bh) {
    List<String> rpcMsgList = helloService.repeat("rpc msg");
    bh.consume(rpcMsgList);
  }

  @Setup
  public void serverSetup() throws InterruptedException {
    server =
        RpcServer.builder()
            .nThread(8)
            .debugEnabled(false)
            .serverPort(8888)
            .basePackage("org.cyka")
            .build();
    server.start();
    client = NettyRpcClient.builder().basePackage("org.cyka").nThread(8).build();
    helloService = client.getServiceCallerInstance(HelloService.class);
  }

  @TearDown
  public void cleanUp() {
    server.stop();
    client.stop();
  }
}
