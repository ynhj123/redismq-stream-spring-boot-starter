package com.github.ynhj123.redismq.stream.conf;

import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.Collections;

/**
 * @date: 2021-08-04
 * @author: yangniuhaojiang
 * @title: ListenConfig
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Configuration
public class ListenConfig {
    private static final Logger log = LoggerFactory.getLogger(ListenConfig.class);
    @Autowired
    @Qualifier("redisMqTemplate")
    RedisTemplate redisTemplate;

    @Bean
    public Subscription listener(RedisTemplate redisTemplate) {
        createGroup(redisTemplate);
        Subscription subscription = startSubscription(streamListener, redisTemplate);
        return subscription;
    }
    private Subscription startSubscription(RedisTemplate redisTemplate) {
        RedisConnectionFactory redisConnectionFactory = redisTemplate.getConnectionFactory();
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, OrderMessage>> options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(1))
                .targetType(OrderMessage.class)
                .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, OrderMessage>> listenerContainer = StreamMessageListenerContainer
                .create(redisConnectionFactory, options);
        Subscription subscription = listenerContainer.receiveAutoAck(
                Consumer.from("user" + dataCenterId, "user" + dataCenterId),
                StreamOffset.create("order-stream", ReadOffset.lastConsumed()),
                streamListener);

        listenerContainer.start();
        return subscription;
    }

    private void createGroup(RedisTemplate redisTemplate) {
        try {
            redisTemplate.opsForStream().createGroup("order-stream", "user");
        } catch (RedisSystemException e) {
            if (e.getRootCause().getClass().equals(RedisBusyException.class)) {
                log.info("STREAM - Redis group already exists, skipping Redis group creation: order");
            } else if (e.getRootCause().getClass().equals(RedisCommandExecutionException.class)) {
                log.info("STREAM - Stream does not yet exist, creating empty stream: event-stream");
                // TODO: There has to be a better way to create a stream than this!?
                redisTemplate.opsForStream().add("order-stream", Collections.singletonMap("", ""));
                redisTemplate.opsForStream().createGroup("order-stream", "order");
            } else throw e;
        }
    }
}
