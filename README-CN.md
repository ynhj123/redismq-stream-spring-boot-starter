# redismq-stream-spring-boot-starter
基于 Redis Stream 实现的轻量级消息队列 Spring Boot Starter，提供简单易用的 API，支持多种消息模式和高级特性。

## [文档](https://www.jianshu.com/p/b95a265f838a)
[English](README.md)
## 功能特性

- [x] **异步消息**：支持异步发送和消费消息
- [x] **订阅发布**：支持多消费者组订阅同一消息
- [x] **延迟消息**：支持指定延迟时间后触发消息
- [x] **ACK 机制**：支持消息确认和手动提交
- [x] **死信队列**：支持失败消息重试N次自动进入死信队列
- [x] **优雅停机**：支持消费者优雅停止，确保消息不丢失

## 技术方案

### 1. 核心技术栈
- Spring Boot 3.x
- Redis Stream (基于 Lettuce 客户端)
- Java 21

### 2. 架构设计

#### 模块结构
- `core`：核心功能实现，包含自动配置、消息发送、消费等核心逻辑
- `common`：通用测试类和消息实体
- `consumer`：消费者示例
- `producer`：生产者示例

#### 核心实现

##### 消息发送机制
- 普通消息：直接写入 Redis Stream
- 延迟消息：使用 Redis ZSet 存储，定时轮询触发
- 覆盖发送：写入并自动裁剪 Stream 长度

##### 消息消费机制
- 基于 Spring Data Redis StreamListener
- 支持多消费者组，相同组内消息仅消费一次
- 手动 ACK 模式，确保消息可靠消费

##### 延迟消息实现
- 使用 Redis ZSet 存储延迟消息，Score 为过期时间戳
- 定时任务每秒轮询 ZSet，将到期消息移至目标 Stream
- 支持消息幂等处理，避免重复消费

##### 死信队列实现
- 消息消费失败达到最大重试次数后自动进入死信队列
- 死信队列命名规则：`{event}-dlq`
- 支持单独监听和处理死信消息

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.github.ynhj123</groupId>
    <artifactId>redismq-stream-spring-boot-start</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置 Redis

在 `application.properties` 或 `application.yml` 中配置 Redis 连接信息：

```properties
spring.data..redis.host=localhost
spring.data..redis.port=6379
spring.data..redis.password=
spring.data..redis.database=0
```


| 配置项 | 默认值 | 描述               |
| --- | --- |------------------|
| spring.redis.stream.prefix | redismq | Redis Key 前缀     |
| spring.redis.stream.maxLen | 100 | Stream 最大长度，超过自动裁剪 |
| spring.redis.stream.delay.enabled | true | 是否启用延迟消息功能       |

### 3. 发送消息

```java
import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    
    @Autowired
    private RedisStreamMqStartService redisStreamMqStartService;
    
    // 发送普通消息
    public void sendMessage(String message) {
        TestMessage1 testMessage1 = new TestMessage1();
        testMessage1.content = message;
        redisStreamMqStartService.coverSend("message1Listener", testMessage1);
    }
    
    // 发送延迟消息
    public void sendDelayMessage(String message, long delayMillis) {
        TestMessage1 testMessage1 = new TestMessage1();
        testMessage1.content = message;
        redisStreamMqStartService.delaySend("message1Listener", testMessage1, delayMillis);
    }
}
```

### 4. 消费消息

```java
import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RedisStreamMqListen(value = "message1Listener", type = TestMessage1.class)
public class MessageConsumer implements StreamListener<String, ObjectRecord<String, TestMessage1>> {
    
    @Override
    public void onMessage(ObjectRecord<String, TestMessage1> message) {
        // 处理消息
        TestMessage1 msg = message.getValue();
        System.out.println("Received message: " + msg.content);
        
        // 业务处理...
        
        // 注意：Redis Stream 会自动处理 ACK
    }
}
```



## 高级特性

### 1. 消费者组

- 消费者组默认使用 `spring.application.name`
- 不同组可以重复消费同一条消息
- 相同组内消息仅消费一次

### 2. 消息重试

- 默认最大重试次数：3次
- 重试间隔：1秒、2秒、3秒（指数退避）
- 超过最大重试次数后进入死信队列

### 3. 死信队列

- 死信消息包含原始消息和失败信息
- 可以单独监听死信队列：`{event}-dlq`

## 项目介绍

- **core**：核心代码实现
- **common**：测试通用类，包含消息实体
- **consumer**：测试消费者，用于多开测试异步和订阅通知
- **producer**：测试生产者，启动后生成消息并同时消费

---

