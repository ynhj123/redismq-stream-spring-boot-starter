package com.github.ynhj123.redismq.stream.conf;

import com.github.ynhj123.redismq.stream.annotation.RedisStreamMqListen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: ListenAnnotation
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Component
@ConditionalOnBean(RedisStreamMqStartService.class)
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
                redisStreamMqStartService.listener(ca.value(), ca.type(), (StreamListener) bean);
                log.info("event {} start listen", ca.value());
            }

        }
    }

}
