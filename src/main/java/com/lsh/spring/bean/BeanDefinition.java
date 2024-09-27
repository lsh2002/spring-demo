package com.lsh.spring.bean;

/**
 * @author lsh
 * @version 1.0.0
 * @description bean定义
 * @date 2024/9/26 15:05
 **/
public class BeanDefinition {

    private Class clazz;

    private String scope;


    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
