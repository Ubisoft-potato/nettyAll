package org.cyka.service;

import org.cyka.annotation.RpcCaller;

import java.util.List;

@RpcCaller(serviceName = "HelloService")
public interface HelloService {
  String sayHello(String name);

  List<String> repeat(String name);
}
