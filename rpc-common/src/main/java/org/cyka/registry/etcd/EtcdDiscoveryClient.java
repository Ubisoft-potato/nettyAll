package org.cyka.registry.etcd;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.cyka.registry.DiscoveryClient;
import org.cyka.registry.ServiceEndpoint;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class EtcdDiscoveryClient implements DiscoveryClient {
  // ---------------------------constant-----------------------------------------
  private final String SLASH = "/";
  // Guava string splitter
  private final Splitter semicolonSplitter = Splitter.on(':');

  // ----------------------------etcd--------------------------------------------
  // etcd  kv client
  private final KV kv;
  private final Client client;
  private final Watch watch;

  // ----------------------------service map-------------------------------------
  private final ConcurrentMap<String, Set<ServiceEndpoint>> serviceEndpointMap =
      Maps.newConcurrentMap();

  @Override
  public Set<ServiceEndpoint> getServiceEndpoints(String serviceName) {
    checkArgument(!Strings.isNullOrEmpty(serviceName), "serviceName is null or empty");
    return this.serviceEndpointMap.get(serviceName);
  }

  @Override
  public void watchServicesChange(Iterable<String> serviceNames) {
    serviceNames.forEach(
        serviceName -> {
          String strKey =
              MessageFormat.format("/{0}/{1}", EtcdClientHolder.getRootpath(), serviceName);
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

      boolean add = serviceEndpointMap.get(serviceName).add(serviceEndpoint);
      if (add) {
        log.debug(
            "service: {}'s endpoint had been added, service endpoint: {} ",
            serviceName,
            serviceEndpoint);
      }
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

  @Override
  public void disconnect() {
    EtcdClientHolder.disconnect();
  }

  public EtcdDiscoveryClient() {
    this(null);
  }

  public EtcdDiscoveryClient(String registryAddress) {
    this.client = EtcdClientHolder.getOrCreateClient(registryAddress);
    this.kv = client.getKVClient();
    this.watch = client.getWatchClient();
  }
}
