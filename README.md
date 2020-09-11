# nettyAll
netty使用案例，包括httpClient、Rpc等应用场景。

## httpClient

TODO

## Rpc
- 基于Etcd做注册中心（服务注册、发现、负载均衡）
- [kryo]([kryo](https://github.com/EsotericSoftware/kryo)) 实现序列化
- Reflections反射框架实现注解扫描
- JDK Proxy 和 Cglib Proxy（TODO）实现远程方法调用
- 断路器(Circuit Breaker) 集成(TODO) : [Resilience4j](https://resilience4j.readme.io/)
- 客户端开发池化：`NettyChannelPool`、Apache Common Pool(TODO)

