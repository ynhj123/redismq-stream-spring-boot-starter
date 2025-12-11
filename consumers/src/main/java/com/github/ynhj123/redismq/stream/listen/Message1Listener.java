package com.github.ynhj123.redismq.stream.listen;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import com.github.ynhj123.redismq.stream.message.TestMessage1;
import org.apache.commons.lang3.RandomUtils;
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
@RedisStreamMqListen(value = "message1Listener", type = TestMessage1.class)
public class Message1Listener implements StreamListener<String, ObjectRecord<String, TestMessage1>> {
    private static final Logger logger = LoggerFactory.getLogger(Message1Listener.class);

    @Override
    public void onMessage(ObjectRecord<String, TestMessage1> message) {
        int tail = Integer.parseInt(String.valueOf(message.getValue().content.charAt(message.getValue().content.length() - 1)));
        if (tail % 3 == 1) {
            int i = RandomUtils.insecure().randomInt(0, 3);
            switch (i) {
                case 0:
                    logger.info("onMessage: {}", message.getValue().content);
                    break;
                case 1:
                    logger.info("exception onMessage: {}", message.getValue().content);
                    throw new RuntimeException("onMessage: " + message.getValue().content);
                case 2:
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    logger.info("timout onMessage: {}", message.getValue().content);
                    break;
                default:
                    logger.info("onMessage: {}", message.getValue().content);
            }
        } else {
            logger.info("exception onMessage: {}", message.getValue().content);
            throw new RuntimeException("onMessage: " + message.getValue().content);
        }

    }
}
