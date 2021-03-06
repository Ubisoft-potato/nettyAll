<p align="center">
	<img src="https://raw.githubusercontent.com/Ubisoft-potato/pic/master/project_logo.png" width=""/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/netty-4.1.45.Final-brightgreen" width=""/>
  <img src="https://img.shields.io/badge/jetcd-0.5.3-green" width=""/>
  <img src="https://img.shields.io/badge/Guice-4.2.3-yellow" width=""/>
</p>

# 介绍

`Netty`应用场景探索：

- [x] `Rpc`
- [ ] `HttpClient`
- [ ] `HttpServer`

# 模块

## Rpc

<p align="center">
	<img src="https://raw.githubusercontent.com/Ubisoft-potato/pic/master/rpc.jpg" width=""/>
</p>

- [x] 基于`Etcd`做注册中心（服务注册、发现、负载均衡）
- [x] 集成[Guice](https://github.com/google/guice) (`Spring`很全面，`Guice`专注`ioC`，更轻量)
- [x] [kryo](https://github.com/EsotericSoftware/kryo) 实现序列化
- [x] `Reflections`反射框架实现注解扫描
- [x] `JDK Proxy`
- [ ]  `Cglib Proxy`
- [ ] 断路器(`Circuit Breaker`) 集成 : [Resilience4j](https://resilience4j.readme.io/)
- [x] 客户端池化：使用`NettyChannelPool`
- [ ] 客户端池化：使用`Apache Common Pool`

