# redismq-stream-spring-boot-starter
Lightweight message queue Spring Boot Starter based on Redis Stream, providing simple and easy-to-use API with multiple message patterns and advanced features.

## [Documentation](https://www.jianshu.com/p/b95a265f838a) (Chinese)

[中文版本](README-CN.md)
## Features

- [x] **Asynchronous Messages**: Support sending and consuming messages asynchronously
- [x] **Publish-Subscribe**: Support multiple consumer groups subscribing to the same message
- [x] **Delayed Messages**: Support triggering messages after a specified delay time
- [x] **ACK Mechanism**: Support message acknowledgment and manual commit
- [x] **Dead Letter Queue**: Support automatically moving failed messages to dead letter queue after N retries
- [x] **Graceful Shutdown**: Support graceful stop of consumers to ensure no message loss

## Technical Solution

### 1. Core Technology Stack
- Spring Boot 3.x
- Redis Stream (based on Lettuce client)
- Java 21

### 2. Architecture Design

#### Module Structure
- `core`: Core function implementation, including auto-configuration, message sending, consumption, etc.
- `common`: Common test classes and message entities
- `consumer`: Consumer examples
- `producer`: Producer examples

#### Core Implementation

##### Message Sending Mechanism
- Normal messages: Directly written to Redis Stream
- Delayed messages: Stored using Redis ZSet, triggered by periodic polling
- Cover send: Write and automatically trim Stream length

##### Message Consumption Mechanism
- Based on Spring Data Redis StreamListener
- Support multiple consumer groups, messages in the same group are consumed only once
- Manual ACK mode to ensure reliable message consumption

##### Delayed Message Implementation
- Store delayed messages using Redis ZSet, Score is the expiration timestamp
- Scheduled task polls ZSet every second, moving expired messages to target Stream
- Support idempotent message processing to avoid duplicate consumption

##### Dead Letter Queue Implementation
- Messages automatically enter dead letter queue after maximum retry times are reached
- Dead letter queue naming rule: `{event}-dlq`
- Support separate listening and processing of dead letter messages

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.github.ynhj123</groupId>
    <artifactId>redismq-stream-spring-boot-start</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Redis

Configure Redis connection information in `application.properties` or `application.yml`:

```properties
spring.data..redis.host=localhost
spring.data..redis.port=6379
spring.data..redis.password=
spring.data..redis.database=0
```

| Configuration Item | Default Value | Description |
| --- | --- | --- |
| spring.redis.stream.prefix | redismq | Redis Key prefix |
| spring.redis.stream.maxLen | 100 | Maximum Stream length, automatically trimmed when exceeded |
| spring.redis.stream.delay.enabled | true | Whether to enable delayed message function |

### 3. Send Messages

```java
import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    
    @Autowired
    private RedisStreamMqStartService redisStreamMqStartService;
    
    // Send normal message
    public void sendMessage(String message) {
        TestMessage1 testMessage1 = new TestMessage1();
        testMessage1.content = message;
        redisStreamMqStartService.coverSend("message1Listener", testMessage1);
    }
    
    // Send delayed message
    public void sendDelayMessage(String message, long delayMillis) {
        TestMessage1 testMessage1 = new TestMessage1();
        testMessage1.content = message;
        redisStreamMqStartService.delaySend("message1Listener", testMessage1, delayMillis);
    }
}
```

### 4. Consume Messages

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
        // Process message
        TestMessage1 msg = message.getValue();
        System.out.println("Received message: " + msg.content);
        
        // Business processing...
        
        // Note: Redis Stream automatically handles ACK
    }
}
```

## Advanced Features

### 1. Consumer Groups

- Consumer group defaults to `spring.application.name`
- Different groups can consume the same message repeatedly
- Messages in the same group are consumed only once

### 2. Message Retry

- Default maximum retry times: 3 times
- Retry interval: 1s, 2s, 3s (exponential backoff)
- Enter dead letter queue after exceeding maximum retry times

### 3. Dead Letter Queue

- Dead letter messages contain original messages and failure information
- Can listen to dead letter queue separately: `{event}-dlq`

## Project Introduction

- **core**: Core code implementation
- **common**: Common test classes, including message entities
- **consumer**: Test consumers for multi-instance testing of asynchronous and subscription notifications
- **producer**: Test producer that generates and consumes messages after startup

---

