package com.github.ynhj123.redismq.stream.bean;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.stream.StreamListener;

import java.util.Map;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: ListenAnnotation
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
public class ListenAnnotation implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ListenAnnotation.class);


    final RedisStreamMqStartService redisStreamMqStartService;

    public ListenAnnotation(RedisStreamMqStartService redisStreamMqStartService) {
        this.redisStreamMqStartService = redisStreamMqStartService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            Map<String, Object> beans = event.getApplicationContext().getBeansWithAnnotation(RedisStreamMqListen.class);
            for (Object bean : beans.values()) {
                RedisStreamMqListen ca = bean.getClass().getAnnotation(RedisStreamMqListen.class);
                String eventName = ca.value();
                int maxAttempts = ca.maxAttempts();
                boolean isDeadLetterQueue = ca.isDeadLetterQueue();
                
                // 如果是死信队列，使用死信队列的event名称
                if (isDeadLetterQueue) {
                    eventName = eventName + "-dlq";
                }
                
                redisStreamMqStartService.listener(eventName, ca.type(), (StreamListener) bean, maxAttempts);
                log.info("event {} start listen, maxAttempts: {}, isDeadLetterQueue: {}", eventName, maxAttempts, isDeadLetterQueue);
            }

        }
    }

}
