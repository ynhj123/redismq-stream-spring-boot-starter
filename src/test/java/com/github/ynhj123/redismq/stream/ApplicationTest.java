package com.github.ynhj123.redismq.stream;

import com.github.ynhj123.redismq.stream.conf.RedisStreamMqStartService;
import com.github.ynhj123.redismq.stream.message.TestMessage1;
import com.github.ynhj123.redismq.stream.message.TestMessage2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: ApplicationTest
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@SpringBootTest
public class ApplicationTest {
    final String event1 = "message1Listener";
    final String event2 = "message2Listener";
    @Autowired
    RedisStreamMqStartService redisStreamMqStartService;

    @Test
    public void sendMsg() throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            if (i % 2 == 1) {
                TestMessage1 testMessage1 = new TestMessage1();
                testMessage1.content = "testMessage1:" + i;
                redisStreamMqStartService.coverSend(event1, testMessage1);
            } else {
                TestMessage2 testMessage2 = new TestMessage2();
                testMessage2.content = "testMessage2:" + i;
                redisStreamMqStartService.coverSend(event2, testMessage2);
            }
            Thread.sleep(1000l);
        }

    }
}
