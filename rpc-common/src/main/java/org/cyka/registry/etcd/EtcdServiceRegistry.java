package org.cyka.registry.etcd;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.ServiceEndpoint;
import org.cyka.registry.ServiceRegistry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;

/** etcd 键值存储注册中心 */
@Slf4j
public class EtcdServiceRegistry implements ServiceRegistry {

  // ---------------------------constant-----------------------------------------
  // 跟路径
  private static final String ROOTPATH = "cykaRpc";
  // 注册中心地址
  private static final String DEFAULT_ADDRESS = "http://127.0.0.1:2379";
  // 租期
  private static final int LeaseTTL = 60;
  private final String SLASH = "/";
  // Guava string splitter
  private final Splitter semicolonSplitter = Splitter.on(':');

  // ----------------------------etcd--------------------------------------------
  // 租赁id
  private Long leaseId;
  // etcd  kv client
  private final KV kv;
  // lease: 租期，用于保持和etcd链接
  private final Lease lease;
  private final Client client;
  private CloseableClient keepAliveClient;
  private final Watch watch;

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
  public Set<ServiceEndpoint> getServiceEndpoints(String serviceName) {
    checkArgument(!Strings.isNullOrEmpty(serviceName), "serviceName is null or empty");
    return this.serviceEndpointMap.get(serviceName);
  }

  @Override
  public void watchServicesChange(Iterable<String> serviceNames) {
    serviceNames.forEach(
        serviceName -> {
          String strKey = MessageFormat.format("/{0}/{1}", ROOTPATH, serviceName);
          ByteSequence serviceNameSequence = ByteSequence.from(strKey, Charsets.UTF_8);
          WatchOption watchOption =
              WatchOption.newBuilder().withPrefix(serviceNameSequence).build();
          watch.watch(
              serviceNameSequence,
              watchOption,
              watchResponse ->
                  watchResponse
                      .getEvents()
                      .forEach(
                          watchEvent -> {
                            switch (watchEvent.getEventType()) {
                              case PUT:
                                handlePutEvent(watchEvent, serviceName);
                                break;
                              case DELETE:
                                handleDeleteEvent(watchEvent, serviceName);
                                break;
                              case UNRECOGNIZED:
                                log.warn(
                                    "unrecognized event, service endpoint: {}",
                                    getServiceEndpointFromKeyValue(watchEvent.getKeyValue()));
                                break;
                            }
                          }),
              throwable -> log.warn("watch error occur: {}", throwable.getMessage()));
        });
  }

  @Override
  public void disconnect() {
    if (Objects.nonNull(keepAliveClient) && Objects.nonNull(client)) {
      keepAliveClient.close();
      client.close();
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

  private void handlePutEvent(WatchEvent watchEvent, String serviceName) {
    ServiceEndpoint serviceEndpoint = getServiceEndpointFromKeyValue(watchEvent.getKeyValue());
    if (!serviceEndpointMap.containsKey(serviceName)) {
      CopyOnWriteArraySet<ServiceEndpoint> endpointSet = Sets.newCopyOnWriteArraySet();
      endpointSet.add(serviceEndpoint);
      serviceEndpointMap.put(serviceName, endpointSet);
      log.debug(
          "service: {}'s first endpoint had been added, service endpoint : {}",
          serviceName,
          serviceEndpoint);
    } else {
      log.debug(
          "service: {}'s endpoint had been added, service endpoint: {} ",
          serviceName,
          serviceEndpoint);
      serviceEndpointMap.get(serviceName).add(serviceEndpoint);
    }
  }

  private void handleDeleteEvent(WatchEvent watchEvent, String serviceName) {
    ServiceEndpoint serviceEndpoint = getServiceEndpointFromKeyValue(watchEvent.getKeyValue());
    serviceEndpointMap.computeIfPresent(
        serviceName,
        (key, oldValue) -> {
          boolean removed = oldValue.remove(serviceEndpoint);
          if (removed)
            log.debug(
                "service: {}'s endpoint: {} had been removed , the last service endpoint list:{} ",
                key,
                serviceEndpoint,
                oldValue);
          return oldValue;
        });
  }

  private ServiceEndpoint getServiceEndpointFromKeyValue(KeyValue keyValue) {
    String serviceKey = keyValue.getKey().toString(Charsets.UTF_8);
    log.debug("etcd service key: {}", serviceKey);
    int index = serviceKey.lastIndexOf(SLASH);
    String serviceUri = serviceKey.substring(index + 1);
    List<String> hostAndPort = semicolonSplitter.splitToList(serviceUri);
    return new ServiceEndpoint(hostAndPort.get(0), Integer.valueOf(hostAndPort.get(1)));
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
    this(DEFAULT_ADDRESS, LeaseTTL);
  }

  private EtcdServiceRegistry(String registryAddress) {
    this(registryAddress, LeaseTTL);
  }

  public EtcdServiceRegistry(String registryAddress, int leaseTTL) {
    registryAddress = registryAddress != null ? registryAddress : DEFAULT_ADDRESS;
    // build etcd client
    this.client = Client.builder().endpoints(registryAddress).build();
    this.kv = client.getKVClient();
    this.lease = client.getLeaseClient();
    this.watch = client.getWatchClient();
    try {
      this.leaseId = lease.grant(leaseTTL).get().getID();
    } catch (InterruptedException | ExecutionException e) {
      log.error(e.getLocalizedMessage());
      e.printStackTrace();
    }
    keepAliveWithEtcd();
  }
}
