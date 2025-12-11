package com.github.ynhj123.redismq.stream.bean;

import org.springframework.data.redis.stream.StreamListener;

/**
 * @date: 2021-08-06
 * @author: yangniuhaojiang
 * @title: RedisStreamMqStartService
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
public interface RedisStreamMqStartService {
    void listener(String event, Class type, StreamListener streamListener);

    void listener(String event, Class type, StreamListener streamListener, int maxAttempts);

    <V> void coverSend(String event, V val);

    <V> void delaySend(String event, V val, long delayMillis);
}
