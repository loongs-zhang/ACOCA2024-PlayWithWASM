# Play with WASM

## 目录

- [诞生之因](#诞生之因)
- [方案选择](#方案选择)
- [本地转发](#本地转发)
- [动态链接库](#动态链接库)
- [WASM](#WASM)
- [待完善的地方](#待完善的地方)

## 诞生之因

Dubbo的SPI扩展只能使用Java语言编写，dubbo-wasm模块旨在克服这一限制。

## 方案选择

| 方案      | 本地转发                                                                  | 多语言SDK                                         | WASM                                               | 动态链接库                                                                                  |
|---------|-----------------------------------------------------------------------|------------------------------------------------|----------------------------------------------------|----------------------------------------------------------------------------------------|
| 方案实现难度  | 其他语言作为服务端需要监听一个额外的端口，Java则作为网络请求的客户端，向端口发送数据                          | 火葬场                                            | 中，由于WASM只能通过几种类型来跟java交互，SDK实现时需要做大量参数交互的映射        | Java层面的SDK容易实现，只要定义native方法、异常等等规范即可                                                   |
| 典型例子    | [apisix](https://github.com/apache/apisix)                            | [dubbo-go](https://github.com/apache/dubbo-go) | [higress](https://github.com/alibaba/higress)      | [opendal](https://github.com/apache/opendal) / [netty](https://github.com/netty/netty) |
| 新语言扩展难度 | 无                                                                     | 火葬场                                            | 增加新语言打WASM库的脚本或代码即可                                | 增加新语言打动态链接库的脚本或代码即可                                                                    |
| 开发难度    | 低                                                                     | 低                                              | 中，需要熟悉跨平台开发(unix/windows)                          | 高，需要额外学习JNI相关知识，而且需要熟悉跨平台开发(unix/windows)                                              |
| 优点      | 通过网络交互完全解耦java和底层                                                     | 既对开发者友好，又没有性能损耗                                | 性能开销相对较低                                           | 无性能损耗，跟Java生态融合很好，在底层创建Java对象、直接读取Java对象的值不是梦                                          |
| 典型缺点    | 极限场景下会有约20%的性能损耗(可参考《深入理解Linux网络：修炼底层内功，掌握高性能原理》)，而且如何本地部署其他语言的服务也是问题 | 火葬场级别的工作量                                      | 不同语言，打成WASM库的方式都不一样，无法统一；另外每次跟WASM交互都会有序列化&反序列化的损耗 | 不同语言，打成动态链接库的方式都不一样，无法统一，而且有些语言无法打成动态链接库                                               |

## 本地转发

todo给出代码示例

## 动态链接库

todo给出代码示例

https://github.com/alibaba/arthas/issues/1920

JNI学习：https://www.imooc.com/learn/1212

## WASM

https://github.com/apache/dubbo-spi-extensions/blob/master/dubbo-wasm/README_zh.md

## 待完善的地方

目前[dubbo-wasm](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-wasm)
是基于[wasmtime-java](https://github.com/kawamuray/wasmtime-java)
实现的，但是wasmtime-java仅支持linux_aarch64、linux_x86_64、macos_aarch64、macos_x86_64、windows_x86_64这几种平台，跨平台方便还有待加强，可以增加一个wasm-runtime的SPI并且用[chicory](https://github.com/dylibso/chicory)
实现；

实现的SPI比较丑，可以考虑代码生成。
