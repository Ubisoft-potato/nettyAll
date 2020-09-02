package org.cyka.pool;

import io.netty.channel.Channel;
import org.cyka.async.AsyncCallback;
import org.cyka.async.AsyncResult;
import org.cyka.registry.ServiceEndpoint;

public interface RpcClientConnectionPool {

  /**
   * get service endpoint's channel from the connection pool
   *
   * @return channel
   * @param serviceEndpoint service endpoint to be acquired
   */
  AsyncResult<Channel> acquireChannel(ServiceEndpoint serviceEndpoint);

  /**
   * get service endpoint's channel from the connection pool, and can get value from the callback
   * function
   *
   * @param serviceEndpoint service endpoint to be acquired
   * @param callback the callback function
   * @return channel
   */
  AsyncResult<Channel> acquireChannel(
      ServiceEndpoint serviceEndpoint, AsyncCallback<Channel> callback);

  /**
   * return the channel to pool
   *
   * @param serviceEndpoint the service endpoint that the channel belong to
   * @param channel the channel to return
   * @param callback the callback to be invoke
   * @return asyncResult
   */
  AsyncResult<Void> releaseChannel(
      ServiceEndpoint serviceEndpoint, Channel channel, AsyncCallback<Void> callback);

  /** shutdown the connection pools */
  void close();
}
