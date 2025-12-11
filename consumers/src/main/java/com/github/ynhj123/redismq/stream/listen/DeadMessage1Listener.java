package com.github.ynhj123.redismq.stream.listen;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import com.github.ynhj123.redismq.stream.entity.DeadLetterMessage;
import com.github.ynhj123.redismq.stream.message.TestMessage1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: com.github.ynhj123.redismq.stream.listen.Message1Listener
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Component
@RedisStreamMqListen(value = "message1Listener", type = TestMessage1.class, isDeadLetterQueue = true, maxAttempts = 3)
public class DeadMessage1Listener implements StreamListener<String, ObjectRecord<String, DeadLetterMessage<TestMessage1>>> {
    private static final Logger logger = LoggerFactory.getLogger(DeadMessage1Listener.class);

    @Override
    public void onMessage(ObjectRecord<String, DeadLetterMessage<TestMessage1>> message) {
        logger.info("DeadMessage1Listener: {}", message.toString());
    }
}
