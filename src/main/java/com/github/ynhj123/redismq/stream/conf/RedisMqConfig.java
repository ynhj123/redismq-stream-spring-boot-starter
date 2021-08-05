package com.github.ynhj123.redismq.stream.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${spring.redis.stream.database}")
    int dataBase = 0;

    @Bean("redisMqTemplate")
    public RedisTemplate redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        lettuceConnectionFactory.setDatabase(dataBase);
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
