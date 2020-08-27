package org.cyka.registry.etcd;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.ServiceRegistry;
import org.cyka.registry.lb.LoadBalanceStrategy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/** etcd 键值存储注册中心 */
@Slf4j
public class EtcdServiceRegistry implements ServiceRegistry {

  // ---------------------------constant------------------------------------------------
  // 跟路径
  private static final String ROOTPATH = "cykaRpc";
  // 注册中心地址
  private static final String DEFAULT_ADDRESS = "http://127.0.0.1:2379";
  // 租期
  private static int LeaseTTL = 60;
  // Guava string splitter
  private final Splitter splitter = Splitter.on(':');

  // ----------------------------etcd---------------------------------------------
  // 租赁id
  private Long leaseId;
  // etcd  kv client
  private final KV kv;
  // lease: 租期，用于保持和etcd链接
  private final Lease lease;
  private final Client client;
  private CloseableClient keepAliveClient;

  @Override
  public void register(String serviceName, Integer port) {
    register(serviceName, getHostIp(), port);
  }

  @Override
  public void register(String serviceName, String ipAddress, Integer port) {
    String strKey =
        MessageFormat.format(
            "/{0}/{1}/{2}:{3}", ROOTPATH, serviceName, ipAddress, String.valueOf(port));
    ByteSequence key = ByteSequence.from(strKey, Charsets.UTF_8);
    ByteSequence val = ByteSequence.from("", Charsets.UTF_8);
    // wait for the future
    try {
      kv.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
    } catch (InterruptedException e) {
      log.warn("register has bean interrupt: {}", e.getMessage());
    } catch (ExecutionException e) {
      log.warn("register fail : {}", e.getMessage());
    }
  }

  @Override
  public List<ServiceEndpoint> getServiceEndpoints(String serviceName) {
    checkArgument(!Strings.isNullOrEmpty(serviceName), "serviceName is null or empty");
    String strKey = MessageFormat.format("/{0}/{1}", ROOTPATH, serviceName);
    ByteSequence key = ByteSequence.from(strKey, Charsets.UTF_8);
    try {
      return kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get().getKvs().stream()
          .map(
              keyValue -> {
                String serviceKey = keyValue.getKey().toString(Charsets.UTF_8);
                int index = serviceKey.lastIndexOf("/");
                String serviceUri = serviceKey.substring(index + 1);
                List<String> hostAndPort = splitter.splitToList(serviceUri);
                return new ServiceEndpoint(hostAndPort.get(0), Integer.valueOf(hostAndPort.get(1)));
              })
          .collect(Collectors.toList());
    } catch (InterruptedException e) {
      log.warn("find service: {}, has bean interrupt: {}", serviceName, e.getMessage());
    } catch (ExecutionException e) {
      log.warn("find service:{} , fail : {}", serviceName, e.getMessage());
    }
    return Lists.newArrayList();
  }

  @Override
  public ServiceEndpoint choose(String serviceName, LoadBalanceStrategy strategy) {
    return null;
  }

  @Override
  public void disconnect() {
    if (Objects.nonNull(keepAliveClient) && Objects.nonNull(client)) {
      keepAliveClient.close();
      client.close();
    }
  }

  /**
   * get local Ip Address : it may be local area network ip (not the public ip)
   *
   * @return string
   */
  private String getHostIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      log.warn("cannot get the local host ip , using localhost as service ip");
    }
    return "127.0.0.1";
  }

  private void keepAliveWithEtcd() {
    keepAliveClient =
        lease.keepAlive(
            leaseId,
            new StreamObserver<LeaseKeepAliveResponse>() {
              @Override
              public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
                log.info(
                    "lease id :{} ,new lease ttl : {}",
                    leaseKeepAliveResponse.getID(),
                    leaseKeepAliveResponse.getTTL());
              }

              @Override
              public void onError(Throwable throwable) {
                log.error("keep alive occurs error: {}", throwable.getMessage());
              }

              @Override
              public void onCompleted() {
                log.debug("finish keep alive");
              }
            });
  }

  public EtcdServiceRegistry() {
    this(DEFAULT_ADDRESS);
  }

  public EtcdServiceRegistry(String registryAddress) {
    registryAddress = registryAddress != null ? registryAddress : DEFAULT_ADDRESS;
    // build etcd client
    this.client = Client.builder().endpoints(registryAddress).build();
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    try {
      this.leaseId = lease.grant(LeaseTTL).get().getID();
    } catch (InterruptedException | ExecutionException e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    keepAliveWithEtcd();
  }
}
