package com.lsh.service;

import com.lsh.spring.ApplicationContext;

import java.net.URISyntaxException;

/**
 * @author lsh
 * @version 1.0.0
 * @description
 * @date 2024/9/26 13:51
 **/
public class Test {

    public static void main(String[] args) throws URISyntaxException {
        ApplicationContext applicationContext = new ApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();
    }
}
