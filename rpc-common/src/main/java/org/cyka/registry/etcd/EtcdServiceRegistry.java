package org.cyka.registry.etcd;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.ServiceRegistry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/** etcd 键值存储注册中心 */
@Slf4j
public class EtcdServiceRegistry implements ServiceRegistry {

  // ---------------------------constant-----------------------------------------
  // 租期
  private static final int LeaseTTL = 60;

  // ----------------------------etcd--------------------------------------------
  // 租赁id
  private Long leaseId;
  // etcd  kv client
  private final KV kv;
  // lease: 租期，用于保持和etcd链接
  private final Lease lease;
  private final Client client;
  private CloseableClient keepAliveClient;

  // ----------------------------service map-------------------------------------
  private final ConcurrentMap<String, Set<ServiceEndpoint>> serviceEndpointMap =
      Maps.newConcurrentMap();

  @Override
  public void register(String serviceName, Integer port) {
    register(serviceName, getHostIp(), port);
  }

  @Override
  public void register(String serviceName, String ipAddress, Integer port) {
    String strKey =
        MessageFormat.format(
            "/{0}/{1}/{2}:{3}",
            EtcdClientHolder.getRootpath(), serviceName, ipAddress, String.valueOf(port));
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
  public void disconnect() {
    if (Objects.nonNull(keepAliveClient)) {
      keepAliveClient.close();
      EtcdClientHolder.disconnect();
    }
  }

  // ----------------------------private method-------------------------------------
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

  // -----------------------constructor---------------------------------------

  public EtcdServiceRegistry() {
    this(null, LeaseTTL);
  }

  public EtcdServiceRegistry(String registryAddress) {
    this(registryAddress, LeaseTTL);
  }

  public EtcdServiceRegistry(String registryAddress, int leaseTTL) {
    this.client = EtcdClientHolder.getOrCreateClient(registryAddress);
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    try {
      this.leaseId = lease.grant(leaseTTL).get().getID();
    } catch (InterruptedException | ExecutionException e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    keepAliveWithEtcd();
  }
}
