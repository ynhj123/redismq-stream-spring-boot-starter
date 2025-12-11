package com.github.ynhj123.redismq.stream.listen;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import com.github.ynhj123.redismq.stream.entity.DeadLetterMessage;
import com.github.ynhj123.redismq.stream.message.TestMessage2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: com.github.ynhj123.redismq.stream.listen.Message2Listener
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Component
@RedisStreamMqListen(value = "message2Listener", type = TestMessage2.class, isDeadLetterQueue = true)
public class DeadMessage2Listener implements StreamListener<String, ObjectRecord<String, DeadLetterMessage<TestMessage2>>> {
    private static final Logger logger = LoggerFactory.getLogger(DeadMessage2Listener.class);

    @Override
    public void onMessage(ObjectRecord<String, DeadLetterMessage<TestMessage2>> message) {
        logger.info("onMessage: {}", message);
    }
}
