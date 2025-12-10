# 简洁稳定的延迟消息实现方案

## 设计理念
- 面向中小企业，简洁实用
- 保证代码的简易性和稳定性
- 与现有代码结构保持一致
- 避免过度设计，专注核心功能

## 核心实现方案

### 1. 数据结构设计

| 数据结构 | Key格式 | 用途 |
|---------|---------|------|
| Sorted Set | `delay:{event}` | 存储延迟消息，score为过期时间戳 |
| String | `delay:processing:{messageId}` | 标记消息正在处理，防止重复处理 |

### 2. 核心组件

#### 2.1 延迟消息发送器
- 扩展现有`RedisStreamMqStartService`接口
- 将消息序列化后存入zSet，score为过期时间

#### 2.2 延迟消息处理器
- 单线程定时轮询，每秒检查一次到期消息
- 使用Redis原子操作确保消息只被处理一次
- 将到期消息发送到对应的Redis Stream

### 3. 核心实现代码

#### 3.1 扩展核心服务接口

```java
// RedisStreamMqStartService.java
public interface RedisStreamMqStartService {
    void listener(String event, Class type, StreamListener streamListener);
    <V> void coverSend(String event, V val);
    <V> void delaySend(String event, V val, long delayMillis); // 新增延迟消息方法
}
```

#### 3.2 实现延迟消息发送

```java
// RedisStreamMqStartServiceImpl.java
public <V> void delaySend(String event, V val, long delayMillis) {
    // 序列化消息
    String messageJson = JSON.toJSONString(val);
    // 计算过期时间戳
    long expireTime = System.currentTimeMillis() + delayMillis;
    // 存入zSet，key为delay:{event}
    String delayKey = "delay:" + event;
    redisTemplate.opsForZSet().add(delayKey, messageJson, expireTime);
    log.info("event {} delay send content {} after {} ms", event, val, delayMillis);
}
```

#### 3.3 实现延迟消息处理器

```java
// DelayMessageProcessor.java
@Component
public class DelayMessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(DelayMessageProcessor.class);
    private final StringRedisTemplate redisTemplate;
    private final long maxLen;
    
    // 构造函数
    
    @PostConstruct
    public void init() {
        // 启动定时任务，每秒执行一次
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::processDelayMessages, 0, 1000, TimeUnit.MILLISECONDS);
    }
    
    private void processDelayMessages() {
        try {
            // 获取所有延迟消息的key
            Set<String> delayKeys = redisTemplate.keys("delay:*");
            if (delayKeys == null || delayKeys.isEmpty()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            for (String delayKey : delayKeys) {
                // 跳过处理中的标记key
                if (delayKey.startsWith("delay:processing:")) {
                    continue;
                }
                
                // 获取到期消息（score <= 当前时间）
                Set<String> expiredMessages = redisTemplate.opsForZSet().rangeByScore(delayKey, 0, currentTime);
                if (expiredMessages == null || expiredMessages.isEmpty()) {
                    continue;
                }
                
                // 处理每条到期消息
                String event = delayKey.substring(6); // 移除"delay:"前缀
                for (String messageJson : expiredMessages) {
                    processSingleMessage(event, messageJson, delayKey);
                }
            }
        } catch (Exception e) {
            log.error("Error processing delay messages", e);
        }
    }
    
    private void processSingleMessage(String event, String messageJson, String delayKey) {
        // 生成唯一消息ID
        String messageId = UUID.randomUUID().toString();
        String processingKey = "delay:processing:" + messageId;
        
        try {
            // 尝试标记消息正在处理（SETNX，10秒过期）
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(processingKey, "processing", 10, TimeUnit.SECONDS);
            if (locked != null && locked) {
                // 从zSet中移除消息（原子操作）
                Long removed = redisTemplate.opsForZSet().remove(delayKey, messageJson);
                if (removed != null && removed > 0) {
                    // 反序列化消息
                    Object val = JSON.parse(messageJson);
                    // 发送到对应的Redis Stream
                    ObjectRecord<String, Object> record = StreamRecords.newRecord()
                            .ofObject(val)
                            .withId(RecordId.autoGenerate())
                            .withStreamKey(event);
                    redisTemplate.opsForStream().add(record);
                    redisTemplate.opsForStream().trim(event, maxLen, true);
                    log.info("event {} delay message processed: {}", event, val);
                }
            }
        } finally {
            // 移除处理标记
            redisTemplate.delete(processingKey);
        }
    }
}
```

### 4. 自动配置

```java
// RedisStreamMqAutoConfigure.java
@Bean
public DelayMessageProcessor delayMessageProcessor(StringRedisTemplate redisTemplate) {
    return new DelayMessageProcessor(redisTemplate, maxLen);
}
```

## 实现步骤

1. **扩展核心服务接口**：在`RedisStreamMqStartService`中新增`delaySend`方法
2. **实现延迟消息发送**：在`RedisStreamMqStartServiceImpl`中实现`delaySend`方法
3. **创建延迟消息处理器**：实现`DelayMessageProcessor`类，包含定时轮询逻辑
4. **配置自动装配**：在`RedisStreamMqAutoConfigure`中添加延迟消息处理器的配置
5. **添加依赖**：确保项目已引入Jackson等必要依赖
6. **编写测试用例**：验证延迟消息功能

## 配置项设计

| 配置项 | 默认值 | 说明 |
|---------|---------|------|
| spring.redis.stream.delay.enabled | true | 是否启用延迟消息功能 |
| spring.redis.stream.delay.poll-interval | 1000 | 轮询间隔（毫秒） |
| spring.redis.stream.delay.processing-timeout | 10000 | 消息处理超时时间（毫秒） |

## 优势分析

1. **简洁实用**：代码结构简单，易于理解和维护
2. **稳定可靠**：单线程处理，避免并发问题
3. **与现有代码保持一致**：遵循项目现有设计风格
4. **无额外依赖**：仅使用Redis和Spring框架，无需引入新依赖
5. **适合中小企业**：满足大部分中小企业的延迟消息需求
6. **易于扩展**：可以根据需要逐步扩展功能

## 预期效果

1. 实现延迟消息功能，支持发送延迟消息
2. 消息到期后自动发送到对应的Redis Stream
3. 确保消息只被处理一次
4. 代码简洁，易于维护
5. 稳定可靠，适合中小企业使用

## 技术要点

1. **Redis zSet**：使用Sorted Set存储延迟消息，按过期时间排序
2. **定时轮询**：使用ScheduledExecutorService实现定时任务
3. **原子操作**：使用SETNX确保消息只被处理一次
4. **异常处理**：完善的异常处理机制，确保系统稳定运行
5. **简洁设计**：避免过度设计，专注核心功能

通过这种简洁、稳定的设计方案，可以满足中小企业的延迟消息需求，同时保证代码的简易性和稳定性，符合项目的定位和用户的期望。