<img src="https://i0.hdslb.com/bfs/album/ee28bce0f423e12882e335a9b94c3b56b8418439.png" height="200px" alt="">

# Ginkgo (银杏)
点滴梦想编织成诗

## 🍁 简介
Ginkgo是基于nio编写的非阻塞通信框架，它包含客户端和服务端两部分，它实现了大部分的通信
功能，开发者只需关心需要传输的数据和服务端、客户端需要对数据如何处理，它完全去除了配置文件
而是采用注解开发。它同Dandelion的设计理念类似，都是为了简化开发流程。

## 👒 导入依赖
服务端
```html
<repositories>
    <repository>
        <id>ginkgo-mvn-repo</id>
        <url>https://raw.githubusercontent.com/Ketuer/Ginkgo/main/repo</url>
    </repository>
</repositories>

<dependencies>     
    <!-- 服务端框架 -->
    <dependency>
        <groupId>net.ginkgo</groupId>
        <artifactId>ginkgo-server</artifactId>
        <version>1.0-Release</version>
    </dependency>
    
    <!-- 客户端框架 -->
    <dependency>
        <groupId>net.ginkgo</groupId>
        <artifactId>ginkgo-client</artifactId>
        <version>1.0-Release</version>
    </dependency>
</dependencies>
```

## 🌴 整体架构
服务端采用服务机制，客户端采用数据包监听器机制，通过通道进行双向数据传输，整体架构图：
![Markdown](https://i0.hdslb.com/bfs/album/640e46ae6a1ba7fe8f3a44042937faa9718d0309.jpg)

### 服务端
服务端接收数据后，首先进行解压操作，再根据设置的数据包加密算法进行解密（默认采用RSA非对称加密，
采用加密算法能够更好地保护数据传输过程中的安全性）最后对数据进行对象封装，并分发给处理对应数据包
的服务。客户端若首次连接，会发送一个握手请求，此时服务端会返回一个握手数据包，包含加密算法和服
务端相关信息。整体流程示意图如下：
![](https://i0.hdslb.com/bfs/album/4b4531a72a6c628c92ade24b63750033cea2df55.jpg)

### 客户端
客户端会在第一次连接是发送握手请求，直到收到服务端的握手数据包才能发送其他的数据包给服务端，
客户端采用监听机制，收到对应类型的数据包，只调用对应数据包的监听器（一种数据包可以有N个监听器）
客户端同样包含数据包解压/压缩和解密/加密机制，与服务端一致，保证数据传输能够安全可靠。