package com.lsh.service;

import com.lsh.spring.annotation.Autowired;
import com.lsh.spring.annotation.Component;
import com.lsh.spring.bean.BeanNameAware;
import com.lsh.spring.bean.InitializingBean;

/**
 * @author lsh
 */
@Component("userService")
public class UserServiceImpl implements UserService, BeanNameAware, InitializingBean {

    @Autowired
    private OrderService orderService;

    private String beanName;

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("初始化...afterPropertiesSet");
    }

    @Override
    public void test(){
        System.out.println(orderService);
    }
}
