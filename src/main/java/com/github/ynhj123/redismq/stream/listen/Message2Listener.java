package com.github.ynhj123.redismq.stream.listen;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import com.github.ynhj123.redismq.stream.message.TestMessage2;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: Message2Listener
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Component
@RedisStreamMqListen(value = "message2Listener", type = TestMessage2.class)
public class Message2Listener implements StreamListener<String, ObjectRecord<String, TestMessage2>> {
    @Override
    public void onMessage(ObjectRecord<String, TestMessage2> message) {
        System.out.println(message.getValue().content);
    }
}
