package com.github.ynhj123.redismq.stream.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Map;

/**
 * @date: 2021-08-05
 * @author: yangniuhaojiang
 * @title: ApplicationContextUtils
 * @version: 1.0
 * @description： update_version: update_date: update_author: update_note:
 */
public final class ApplicationContextUtils {

    private ApplicationContextUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    public static <U> U getBean(ApplicationContext context, Class<U> clz) {
        try {
            U bean = context.getBean(clz);
            return bean;
        } catch (BeansException e) {
            return null;
        }
    }

    public static <U> Collection<U> getBeans(ApplicationContext context, Class<U> clz) {
        try {
            Map<String, U> beanMap = context.getBeansOfType(clz);
            return beanMap.values();
        } catch (BeansException e) {
            // do nothing
        }
        return null;
    }
}
