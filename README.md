# Play with WASM

## 目录

- [自我介绍](#自我介绍)
- [诞生之因](#诞生之因)
- [方案选择](#方案选择)
- [本地转发](#本地转发)
- [动态链接库](#动态链接库)
- [WASM](#WASM)
- [待完善的地方](#待完善的地方)

## 自我介绍

### 大家好，我叫张子成，是一名java/rust开发人员。2021年2月，我与@hengyyunabc合作，在arthas创建了vmtool。2022年4月，我成为shenyu的committer，创建了MemorySafeLinkedBlockingQueue。一个月后，我加入了shenyu PMC团队，开始建造一个名为open-coroutine的有栈协程库。2023年4月，我为open-coroutine实现了抢占式调度功能。2024年1月，我在Apache ShenYu中支持了WASM插件，并迅速将这一想法传播到其他社区，如dubbo。

## 诞生之因

Dubbo的SPI扩展只能使用Java语言编写，dubbo-wasm模块旨在克服这一限制。

## 方案选择

| 方案      | 本地转发                                                                                    | 多语言SDK                                         | WASM                                               | 动态链接库                                                                                  |
|---------|-----------------------------------------------------------------------------------------|------------------------------------------------|----------------------------------------------------|----------------------------------------------------------------------------------------|
| 方案实现难度  | 中等偏易，其他语言作为服务端需要监听一个额外的端口，Java则作为网络请求的客户端，向端口发送数据                                       | 火葬场                                            | 中，由于WASM只能通过几种类型来跟java交互，SDK实现时需要做大量参数交互的映射        | Java层面的SDK容易实现，只要定义native方法、异常等等规范即可                                                   |
| 典型例子    | [apisix](https://github.com/apache/apisix)                                              | [dubbo-go](https://github.com/apache/dubbo-go) | [higress](https://github.com/alibaba/higress)      | [opendal](https://github.com/apache/opendal) / [netty](https://github.com/netty/netty) |
| 新语言扩展难度 | 无                                                                                       | 火葬场                                            | 增加新语言打WASM库的脚本或代码即可                                | 增加新语言打动态链接库的脚本或代码即可                                                                    |
| 开发难度    | 低                                                                                       | 低                                              | 中，需要熟悉跨平台开发(unix/windows)                          | 高，需要额外学习JNI相关知识，而且需要熟悉跨平台开发(unix/windows)                                              |
| 优点      | 通过网络交互完全解耦java和底层                                                                       | 既对开发者友好，又没有性能损耗                                | 性能开销相对较低                                           | 性能损耗极低，跟Java生态融合很好，在底层创建Java对象、直接读取Java对象的值不是梦                                         |
| 典型缺点    | 极限场景下光协议栈就有约20%的性能损耗(可参考《深入理解Linux网络：修炼底层内功，掌握高性能原理》)，再加上序列化带来的性能损耗，而且如何本地部署其他语言的服务也是问题 | 火葬场级别的工作量                                      | 不同语言，打成WASM库的方式都不一样，无法统一；另外每次跟WASM交互都会有序列化&反序列化的损耗 | 不同语言，打成动态链接库的方式都不一样，无法统一，而且有些语言无法打成动态链接库                                               |

## 本地转发

以下是个简单的本地转发SPI实现例子。

local rust server

```rust
use std::io::prelude::*;
use std::net::TcpListener;
use std::net::TcpStream;

fn main() {
    let listener = TcpListener::bind("127.0.0.1:7878").unwrap();
    for stream in listener.incoming() {
        let stream = stream.unwrap();
        handle_connection(stream);
    }
}

fn handle_connection(mut stream: TcpStream) {
    let mut buffer = [0; 512];
    stream.read(&mut buffer).unwrap();
    // Maybe need to deserialize local client requests and
    // convert them into the parameters required for brpc 
    // client to initiate generic calls
    println!("Request: {}", String::from_utf8_lossy(&buffer[..]));
    // The response data may require serialization
    let response = "Hello from local brpc server";
    stream.write(response.as_bytes()).unwrap();
    stream.flush().unwrap();
}
```

java client

```java
public class BrpcPlugin extends AbstractShenyuPlugin {
    //......
    @Override
    protected Mono<Void> doExecute(final ServerWebExchange exchange,
                                   final ShenyuPluginChain chain,
                                   final SelectorData selector,
                                   final RuleData rule) {
        String hostName = "127.0.0.1";
        int portNumber = 7878;
        try (
                Socket socket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {
            // Serialization may be required when initiating requests
            out.println("Hello, local brpc server!");
            String response = in.readLine();
            System.out.println("Server response: " + response);
            // Get the response from local brpc client 
            // and deserialize it then return
        } catch (Exception e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
    //......
}
```

## 动态链接库

### JNI是什么

JNI是`Java Native Interface`的缩写，通过使用`native`关键字定义方法，允许Java与`其他语言`进行交互。

### 如何使用JNI编写应用程序

#### step1.定义native方法

```java
public class Main {
    public static native String helloJni();
}
```

#### step2.生成头文件

我们使用命令生成c语言使用的`头文件`。

```shell
javac -h . Main.java
# 两个命令都可以，但是从JDK10开始javah被废弃
# 因此推荐使用上面的命令
javah Main
```

下面是生成头文件`Main.h`的具体内容：

```c
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class Main */

#ifndef _Included_Main
#define _Included_Main
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     Main
 * Method:    helloJni
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_Main_helloJni
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
```

#### step3.编写native的实现`MainImpl.c`

c part
```c
#include <jni.h>
#include <jni_md.h>
#include <jvmti.h>
#include "Main.h"
JNIEXPORT jstring JNICALL Java_Main_helloJni
        (JNIEnv *env, jclass klass) {
    return env->NewStringUTF("Hello JNI");
}
```

其他学习资料可参考：https://www.imooc.com/learn/1212

#### step4.生成动态链接库

我的`JAVA_HOME`为`/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`，对应生成动态链接库的命令为：

```shell
g++ -I /Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include
-I /Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include/darwin 
-I /Users/admin/Downloads/study/jni/src/main/native 
MainImpl.c -m64 -fPIC -shared -o jni.dylib
```

特别注意：

- `-I`要包含JAVA_HOME`include`文件夹下的全部文件夹，不同平台的`include`子文件夹不一样；
- 如果是`32位`的操作系统，需要把命令中的`-m64`改为`-m32`；
- 不同平台生成的`动态链接库`后缀不同，比如linux是`.so`，mac是`.dylib`、`.so`，windows是`.dll`；

#### step5.加载动态链接库

```java
java.lang.System#load
```

PS：实际使用时，包含native方法的类可能会被打成jar包并上传至maven，这时我们要把生成的动态链接库一并打到jar包里面。在运行时，还需要把动态链接库从jar包中解压出来，然后才能通过`java.lang.System#load`
加载它。

#### step6.调用native方法

直接像调用一个java方法一样调用它就好了，下面附上完整代码：

java part
```java
import java.net.URL;
public class Main {
    static {
        final URL url = Main.class.getResource("jni.dylib");
        System.load(url.getPath());
    }
    public static native String helloJni();
    public static void main(String[] args) {
        System.out.println(helloJni());
    }
}
```

诚如您所见，编写一个使用了JNI的Java程序并不难！

### JNI缺点

使用Java与`动态链接库`交互，Java只是入口，核心还是底层语言(如c/c++/rust)编写的代码，这意味着要我们自己`兼容`不同平台的差异(
如NIO底层系统调用epoll/kqueue/IOCP)。

## WASM

https://github.com/apache/dubbo-spi-extensions/blob/master/dubbo-wasm/README_zh.md

## 待完善的地方

目前[dubbo-wasm](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-wasm)
是基于[wasmtime-java](https://github.com/kawamuray/wasmtime-java)
实现的，但是wasmtime-java仅支持linux_aarch64、linux_x86_64、macos_aarch64、macos_x86_64、windows_x86_64这几种平台，跨平台方便还有待加强，可以增加一个wasm-runtime的SPI并且用[chicory](https://github.com/dylibso/chicory)
实现；

实现的SPI比较丑，可以考虑代码生成。
