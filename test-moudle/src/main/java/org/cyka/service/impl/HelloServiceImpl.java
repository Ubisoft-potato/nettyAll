package org.cyka.service.impl;

import com.google.common.collect.Lists;
import org.cyka.annotation.RpcService;
import org.cyka.service.HelloService;

import java.util.List;

@RpcService(serviceName = "HelloService")
public class HelloServiceImpl implements HelloService {
  @Override
  public String sayHello(String name) {
    return "hello " + name;
  }

  @Override
  public List<String> repeat(String name) {
    return Lists.newArrayList(name, name, name);
  }
}
