package org.cyka.registry;

public interface ServiceRegistry {

  /**
   * register a service to etcd with user define ipAddress
   *
   * @param serviceName service name
   * @param ipAddress ip address
   * @param port service port
   */
  void register(String serviceName, String ipAddress, Integer port);

  /**
   * register a service to etcd , using localAddress
   *
   * @param serviceName service name
   * @param port service port
   */
  void register(String serviceName, Integer port);

  /**
   * unRegister service when server shutdown
   *
   * @param serviceName service name
   * @param ipAddress ip address
   * @param port service port
   */
  void unRegister(String serviceName, String ipAddress, Integer port);

  /** when stop supply service , call this method to make current service offline */
  void disconnect();
}
