package com.github.ynhj123.redismq.stream.bean;

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
import java.io.*;

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

    public RedisStreamMqStartServiceImpl(StringRedisTemplate redisTemplate, String group, Long maxLen) {
        this.redisTemplate = redisTemplate;
        this.group = group;
        this.maxLen = maxLen;
    }


    public void listener(String event, Class type, StreamListener streamListener) {
        createGroup(event);
        startSubscription(event, type, streamListener);
    }

    public <V> void coverSend(String event, V val) {
        ObjectRecord<String, V> record = StreamRecords.newRecord()
                .ofObject(val)
                .withId(RecordId.autoGenerate())
                .withStreamKey(event);
        redisTemplate.opsForStream().add(record);
        redisTemplate.opsForStream().trim(event, maxLen, true);
        log.info("event {} send content {}", event, val);
    }

    @Override
    public <V> void delaySend(String event, V val, long delayMillis) {
        try {
            // 使用Java原生序列化消息
            String serializedMessage = serialize(val);
            // 计算过期时间戳
            long expireTime = System.currentTimeMillis() + delayMillis;
            // 存入zSet，key为delay:{event}
            String delayKey = "delay:" + event;
            redisTemplate.opsForZSet().add(delayKey, serializedMessage, expireTime);
            log.info("event {} delay send content {} after {} ms", event, val, delayMillis);
        } catch (Exception e) {
            log.error("event {} delay send error", event, e);
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

    private void startSubscription(String event, Class type, StreamListener streamListener) {
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
        listenerContainer.receiveAutoAck(
                Consumer.from(group, group + dataCenterId),
                StreamOffset.create(event, ReadOffset.lastConsumed()),
                streamListener);
        listenerContainer.start();
    }

    private void createGroup(String event) {
        try {
            redisTemplate.opsForStream().createGroup(event, group);
        } catch (RedisSystemException e) {
            if (e.getRootCause().getClass().equals(RedisBusyException.class)) {
                log.info("STREAM - Redis group already exists, skipping Redis group creation: order");
            } else if (e.getRootCause().getClass().equals(RedisCommandExecutionException.class)) {
                log.info("STREAM - Stream does not yet exist, creating empty stream: event-stream");
                // TODO: There has to be a better way to create a stream than this!?
                redisTemplate.opsForStream().add(event, Collections.singletonMap("", ""));
                redisTemplate.opsForStream().createGroup(event, group);
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
