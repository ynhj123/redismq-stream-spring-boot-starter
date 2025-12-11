package com.github.ynhj123.redismq.stream.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @date: 2025-12-10
 * @author: AI Assistant
 * @title: DelayMessageProcessor
 * @version: 1.0
 * @description： 延迟消息处理器，负责处理到期的延迟消息
 */
@Component
@ConditionalOnProperty(name = "spring.redis.stream.delay.enabled", havingValue = "true", matchIfMissing = true)
public class DelayMessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(DelayMessageProcessor.class);
    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService executor;
    private final long pollInterval = 1000; // 轮询间隔，1秒
    private final long processingTimeout = 10000; // 消息处理超时时间，10秒
    @Value("${spring.redis.stream.maxLen:100}")
    long maxLen = 1000;
    @Value("${spring.redis.stream.prefix:redismq}")
    private String prefix;


    @Autowired
    public DelayMessageProcessor(StringRedisTemplate redisTemplate, RedisStreamMqStartService redisStreamMqStartService) {
        this.redisTemplate = redisTemplate;
        // 通过反射获取maxLen字段值

        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.init();
    }

    /**
     * 获取带前缀的完整Redis键
     */
    private String getFullKey(String event) {
        if (prefix == null || prefix.isEmpty()) {
            return event;
        }
        return prefix + ":" + event;
    }

    private void init() {
        // 启动定时任务，每秒执行一次
        executor.scheduleAtFixedRate(this::processDelayMessages, 0, pollInterval, TimeUnit.MILLISECONDS);
        log.info("DelayMessageProcessor started, poll interval: {} ms", pollInterval);
    }

    private void processDelayMessages() {
        try {
            // 获取所有延迟消息的key，格式为delay:*
            Set<String> delayKeys = redisTemplate.keys("delay:*");
            if (delayKeys == null || delayKeys.isEmpty()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            for (String delayKey : delayKeys) {
                // 跳过处理中的标记key（如果有的话）
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

    private void processSingleMessage(String event, String serializedMessage, String delayKey) {
        // 生成唯一消息ID
        String messageId = UUID.randomUUID().toString();
        String processingKey = "delay:processing:" + messageId;

        try {
            // 尝试标记消息正在处理（SETNX，10秒过期）
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(processingKey, "processing", processingTimeout, TimeUnit.MILLISECONDS);
            if (locked != null && locked) {
                // 从zSet中移除消息（原子操作）
                Long removed = redisTemplate.opsForZSet().remove(delayKey, serializedMessage);
                if (removed != null && removed > 0) {
                    // 反序列化消息（Redis Stream会自动根据监听器的类型进行转换）
                    Object messageObj = deserialize(serializedMessage);
                    // 发送到对应的Redis Stream（event已经包含前缀，无需再次添加）
                    ObjectRecord<String, Object> record = StreamRecords.newRecord()
                            .ofObject(messageObj)
                            .withId(RecordId.autoGenerate())
                            .withStreamKey(event);
                    redisTemplate.opsForStream().add(record);
                    redisTemplate.opsForStream().trim(event, maxLen, true);
                    log.info("event {} delay message processed", event);
                }
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", serializedMessage, e);
        } finally {
            // 移除处理标记
            redisTemplate.delete(processingKey);
        }
    }

    // Java原生序列化
    private <V> String serialize(V obj) throws IOException {
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Object must implement Serializable");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // Java原生反序列化
    private <V> V deserialize(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        V obj = (V) ois.readObject();
        ois.close();
        return obj;
    }
}