package com.github.ynhj123.redismq.stream.autoConfigure;

import com.github.ynhj123.redismq.stream.bean.ListenAnnotation;
import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartService;
import com.github.ynhj123.redismq.stream.bean.RedisStreamMqStartServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @date: 2021-08-06
 * @author: yangniuhaojiang
 * @title: RedisStreamMqAutoConfigure
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Configuration
public class RedisStreamMqAutoConfigure {
    @Value("${spring.application.name:default}")
    private String group;
    @Value("${spring.redis.stream.maxLen:100}")
    long maxLen = 1000;
    @Value("${spring.redis.stream.prefix:redismq}")
    private String prefix;

    @Bean
    @ConditionalOnMissingBean(RedisStreamMqStartService.class)
    public RedisStreamMqStartService redisStreamMqStartService(StringRedisTemplate stringRedisTemplate) {
        return new RedisStreamMqStartServiceImpl(stringRedisTemplate, group, maxLen, prefix);
    }

    @Bean
    @ConditionalOnBean(RedisStreamMqStartService.class)
    public ListenAnnotation listenAnnotation(RedisStreamMqStartService redisStreamMqStartService) {
        return new ListenAnnotation(redisStreamMqStartService);
    }
}
