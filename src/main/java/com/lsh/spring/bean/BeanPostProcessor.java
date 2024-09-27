package com.lsh.spring.bean;

/**
 * bean后置处理器
 *
 * @author lsh
 */
public interface BeanPostProcessor {

    /**
     * 在bean初始化之前调用
     *
     * @param bean bean对象
     * @param beanName bean名称
     * @return bean对象
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * 在bean初始化之后调用
     *
     * @param bean bean对象
     * @param beanName bean名称
     * @return bean对象
     */
    Object postProcessAfterInitialization(Object bean, String beanName);
}
