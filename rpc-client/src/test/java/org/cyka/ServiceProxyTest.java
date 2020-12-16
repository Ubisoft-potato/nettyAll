package org.cyka;

import lombok.extern.slf4j.Slf4j;
import org.cyka.annotation.RpcCaller;
import org.cyka.async.AsyncResult;
import org.cyka.client.NettyRpcClient;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
public class ServiceProxyTest {

  @Test
  @Ignore
  public void rpcCallTest() {
    NettyRpcClient rpcClient = NettyRpcClient.builder().basePackage("org.cyka").nThread(8).build();
    HelloService helloService = rpcClient.getServiceCallerInstance(HelloService.class);
    log.info(helloService.hello());
  }

  @Test
  public void checkReturnType() {
    try {
      final Method method = HelloService.class.getMethod("getSync");
      final Type returnType = method.getGenericReturnType();
      if (returnType instanceof ParameterizedType) {
        ParameterizedType type = (ParameterizedType) returnType;
        System.out.println(type.getRawType().equals(AsyncResult.class));
        Type[] typeArguments = type.getActualTypeArguments();
        for (Type typeArgument : typeArguments) {
          Class<?> typeArgClass = (Class<?>) typeArgument;
          System.out.println("typeArgClass = " + typeArgClass);
        }
      }
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  @RpcCaller(serviceName = "helloService")
  interface HelloService {
    String hello();

    AsyncResult<Integer> getSync();
  }
}
