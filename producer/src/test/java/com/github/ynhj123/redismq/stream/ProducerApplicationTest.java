package com.github.ynhj123.redismq.stream;

import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartService;
import com.github.ynhj123.redismq.stream.message.TestMessage1;
import com.github.ynhj123.redismq.stream.message.TestMessage2;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ProducerApplicationTest {
    private static final Logger log = LoggerFactory.getLogger(ProducerApplicationTest.class);
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

    @Test
    public void delaySendTest() throws InterruptedException {
        // 测试延迟1秒的消息
        TestMessage1 testMessage1 = new TestMessage1();
        testMessage1.content = "delayMessage1:1s";
        redisStreamMqStartService.delaySend(event1, testMessage1, 1000);
        
        // 测试延迟3秒的消息
        TestMessage2 testMessage2 = new TestMessage2();
        testMessage2.content = "delayMessage2:3s";
        redisStreamMqStartService.delaySend(event2, testMessage2, 3000);
        
        // 测试延迟5秒的消息
        TestMessage1 testMessage3 = new TestMessage1();
        testMessage3.content = "delayMessage1:5s";
        redisStreamMqStartService.delaySend(event1, testMessage3, 5000);
        
        // 等待6秒，确保所有延迟消息都已处理
        Thread.sleep(6000);
        
        log.info("Delay message test completed");
    }
}
