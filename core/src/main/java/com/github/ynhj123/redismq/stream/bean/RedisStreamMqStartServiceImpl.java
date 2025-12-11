package com.github.ynhj123.redismq.stream.bean;

import com.github.ynhj123.redismq.stream.entity.DeadLetterMessage;
import com.github.ynhj123.redismq.stream.utils.SerializeUtils;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisException;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;

/**
 * @date: 2021-08-04
 * @author: yangniuhaojiang
 * @title: ListenConfig
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
public class RedisStreamMqStartServiceImpl implements RedisStreamMqStartService {
    private static final Logger log = LoggerFactory.getLogger(RedisStreamMqStartServiceImpl.class);
    private final long dataCenterId = getDataCenterId();

    private final StringRedisTemplate redisTemplate;

    private final String group;
    long maxLen;
    private final String prefix;

    public RedisStreamMqStartServiceImpl(StringRedisTemplate redisTemplate, String group, Long maxLen, String prefix) {
        this.redisTemplate = redisTemplate;
        this.group = group;
        this.maxLen = maxLen;
        this.prefix = prefix;
    }

    /**
     * 获取带前缀的完整Redis键
     */
    private String getFullKey(String event) {
        if (StringUtils.isBlank(prefix)) {
            return event;
        }
        return prefix + ":" + event;
    }


    public void listener(String event, Class type, StreamListener streamListener) {
        listener(event, type, streamListener, 3); // 默认最大重试3次
    }

    @Override
    public void listener(String event, Class type, StreamListener streamListener, int maxAttempts) {
        createGroup(event);
        startSubscription(event, type, streamListener, maxAttempts);
    }

    public <V> void coverSend(String event, V val) {
        String fullKey = getFullKey(event);
        ObjectRecord<String, V> record = StreamRecords.newRecord()
                .ofObject(val)
                .withId(RecordId.autoGenerate())
                .withStreamKey(fullKey);
        redisTemplate.opsForStream().add(record);
        redisTemplate.opsForStream().trim(fullKey, maxLen, true);
        log.info("event {} send content {}", event, val);
    }

    @Override
    public <V> void delaySend(String event, V val, long delayMillis) {
        try {
            // 使用Java原生序列化消息
            String serializedMessage = SerializeUtils.serialize(val);
            // 计算过期时间戳
            long expireTime = System.currentTimeMillis() + delayMillis;
            // 存入zSet，key为delay:{event}
            String delayKey = getFullKey("delay:" + event);
            redisTemplate.opsForZSet().add(delayKey, serializedMessage, expireTime);
            log.info("event {} delay send content {} after {} ms", event, val, delayMillis);
        } catch (Exception e) {
            log.error("event {} delay send error", event, e);
        }
    }

    private <V> void sendToDeadLetterQueue(String event, V val, String reason, int attempts) {
        // 死信队列命名：{event}-dlq
        String queueName = event + "-dlq";
        String dlqEvent = getFullKey(queueName);

        // 创建死信队列的消费者组
        createGroup(queueName);

        // 构建死信消息，包含原始消息和失败信息
        DeadLetterMessage<V> dlqMessage = new DeadLetterMessage<>();
        dlqMessage.setOriginalMessage(val);
        dlqMessage.setReason(reason);
        dlqMessage.setAttempts(attempts);
        dlqMessage.setTimestamp(System.currentTimeMillis());

        // 发送到死信队列
        ObjectRecord<String, DeadLetterMessage<V>> record = StreamRecords.newRecord()
                .ofObject(dlqMessage)
                .withId(RecordId.autoGenerate())
                .withStreamKey(dlqEvent);
        redisTemplate.opsForStream().add(record);
        redisTemplate.opsForStream().trim(dlqEvent, maxLen, true);

        log.info("event {} message sent to DLQ, reason: {}, attempts: {}", event, reason, attempts);
    }


    private void startSubscription(String event, Class type, StreamListener streamListener, int maxAttempts) {
        String fullKey = getFullKey(event);
        RedisConnectionFactory redisConnectionFactory = redisTemplate.getConnectionFactory();
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .targetType(type)
                .errorHandler((t) -> {
                    if (t instanceof RedisSystemException &&
                            t.getCause() instanceof RedisException &&
                            "Connection closed".equals(t.getCause().getMessage())) {
                        // 处理连接关闭的情况
                        log.warn("Connection is already closed");
                    } else {
                        log.error("Unexpected error in Redis stream listener", t);
                    }
                })
                .build();

        StreamMessageListenerContainer listenerContainer = StreamMessageListenerContainer
                .create(redisConnectionFactory, options);

        // 使用手动ACK模式
        listenerContainer.receive(
                Consumer.from(group, group + dataCenterId),
                StreamOffset.create(fullKey, ReadOffset.lastConsumed()),
                new StreamListenerWrapper(streamListener, event, maxAttempts));
        listenerContainer.start();
    }


    // 消息监听器包装类，用于处理重试和死信逻辑
    private class StreamListenerWrapper implements StreamListener<String, ObjectRecord<String, ?>> {
        private final StreamListener<String, ObjectRecord<String, ?>> delegate;
        private final String event;
        private final int maxAttempts;

        public StreamListenerWrapper(StreamListener<String, ObjectRecord<String, ?>> delegate, String event, int maxAttempts) {
            this.delegate = delegate;
            this.event = event;
            this.maxAttempts = maxAttempts;
        }

        @Override
        public void onMessage(ObjectRecord<String, ?> message) {
            RecordId messageId = message.getId();
            Object value = message.getValue();

            // 构建失败计数key
            String failureKey = getFullKey(event + ":failure:" + messageId);

            // 获取当前失败次数
            int attempts = getFailureCount(failureKey);

            // 立即重试逻辑
            while (attempts < maxAttempts) {
                attempts++;
                // 更新失败次数
                updateFailureCount(failureKey, attempts);

                try {
                    // 调用实际的监听器处理消息
                    delegate.onMessage(message);

                    // 处理成功，确认消息并删除失败计数
                    redisTemplate.opsForStream().acknowledge(group, message);
                    redisTemplate.delete(failureKey);
                    log.info("event {} message {} processed successfully on attempt {}", event, messageId, attempts);
                    return;
                } catch (Exception e) {
                    log.error("event {} message {} processing failed, attempt {} of {}", event, messageId, attempts, maxAttempts, e);

                    if (attempts >= maxAttempts) {
                        // 达到最大重试次数，发送至死信队列
                        sendToDeadLetterQueue(event, value, e.getMessage(), attempts);
                        // 确认消息并删除失败计数
                        redisTemplate.opsForStream().acknowledge(group, message);
                        redisTemplate.delete(failureKey);
                        log.warn("event {} message {} sent to DLQ after {} attempts", event, messageId, attempts);
                        return;
                    }

                    // 重试间隔，指数退避
                    try {
                        Thread.sleep(1000L * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("event {} message {} retry interrupted", event, messageId, ie);
                        return;
                    }
                }
            }
        }

        // 获取失败计数
        private int getFailureCount(String failureKey) {
            String countStr = redisTemplate.opsForValue().get(failureKey);
            if (countStr == null) {
                return 0;
            }
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        // 更新失败计数
        private void updateFailureCount(String failureKey, int count) {
            redisTemplate.opsForValue().set(failureKey, String.valueOf(count));
            // 设置过期时间，24小时
            redisTemplate.expire(failureKey, Duration.ofHours(24));
        }
    }

    private void createGroup(String event) {
        String fullKey = getFullKey(event);
        try {
            redisTemplate.opsForStream().createGroup(fullKey, group);
        } catch (RedisSystemException e) {
            if (e.getRootCause().getClass().equals(RedisBusyException.class)) {
                log.info("STREAM - Redis group already exists, skipping Redis group creation: order");
            } else if (e.getRootCause().getClass().equals(RedisCommandExecutionException.class)) {
                log.info("STREAM - Stream does not yet exist, creating empty stream: event-stream");
                // TODO: There has to be a better way to create a stream than this!?
                redisTemplate.opsForStream().add(fullKey, Collections.singletonMap("", ""));
                redisTemplate.opsForStream().createGroup(fullKey, group);
            } else throw e;
        }
    }

    private static Long getDataCenterId() {
        try {
            String hostName = Inet4Address.getLocalHost().getHostName();
            int[] ints = StringUtils.toCodePoints(hostName);
            int sums = 0;
            for (int b : ints) {
                sums += b;
            }
            return (long) (sums % 32);
        } catch (UnknownHostException e) {
            // 如果获取失败，则使用随机数备用
            return RandomUtils.nextLong(0, 31);
        }
    }
}
