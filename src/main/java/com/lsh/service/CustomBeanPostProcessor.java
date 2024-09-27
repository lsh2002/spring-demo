package com.lsh.service;

import java.lang.reflect.Proxy;

import com.lsh.spring.bean.BeanPostProcessor;

/**
 * @author lsh
 * @version 1.0.0
 * @description 自定义bean后置处理器
 * @date 2024/9/27 11:01
 **/
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println(beanName + "...初始化前执行了");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("userService".equals(beanName)) {
            // 创建代理对象
            Object proxyInstance = Proxy.newProxyInstance(CustomBeanPostProcessor.class.getClassLoader(),
                bean.getClass().getInterfaces(), (proxy, method, args) -> {
                    System.out.println("切面逻辑");
                    return method.invoke(bean, args);
                });
            return proxyInstance;
        }
        return bean;
    }
}
