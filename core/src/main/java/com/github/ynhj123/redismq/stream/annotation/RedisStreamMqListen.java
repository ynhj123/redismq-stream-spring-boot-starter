package com.github.ynhj123.redismq.stream.annotation;

import java.lang.annotation.*;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: RedisStreamMqListen
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RedisStreamMqListen {
    String value();

    Class type();
}
