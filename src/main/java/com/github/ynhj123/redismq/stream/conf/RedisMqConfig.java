package com.github.ynhj123.redismq.stream.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @date: 2021-08-04
 * @author: yangniuhaojiang
 * @title: RedisMqConfig
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Configuration
public class RedisMqConfig {

    @Autowired
    RedisConfiguration redisConfiguration;

    @Bean("mqLettuceConnectionFactory")
    public RedisConnectionFactory getLettuceConnectionFactory(RedisConfiguration redisConfiguration) {
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfiguration);
        lettuceConnectionFactory.setValidateConnection(true);
        return lettuceConnectionFactory;
    }

    @Bean("redisMQTemplate")
    public RedisTemplate redisTemplate(RedisConnectionFactory mqLettuceConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(mqLettuceConnectionFactory);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
