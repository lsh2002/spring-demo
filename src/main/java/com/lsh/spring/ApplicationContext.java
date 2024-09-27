package com.lsh.spring;

import com.lsh.spring.annotation.Component;
import com.lsh.spring.annotation.ComponentScan;
import com.lsh.spring.annotation.Scope;
import com.lsh.spring.bean.BeanDefinition;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lsh
 * @version 1.0.0
 * @description
 * @date 2024/9/26 13:56
 **/
public class ApplicationContext {

    private Class config;

    /**
     * bean定义map
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 单例池
     */
    private ConcurrentHashMap<String, Object> singletonMap = new ConcurrentHashMap<>();

    public ApplicationContext(Class configClass) throws URISyntaxException {
        this.config = configClass;

        // 扫描
        // 获取扫描路径
        if (config.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = (ComponentScan) config.getAnnotation(ComponentScan.class);
            String path = componentScan.value();
            // 转换为文件路径
            String filePath = path.replace(".", "/");

            ClassLoader classLoader = this.getClass().getClassLoader();
            URI resource = Objects.requireNonNull(classLoader.getResource(filePath)).toURI();

            // 根据路径获取文件夹
            File file = new File(resource.getPath());
            // 判断是否为文件夹
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    // 存在文件
                    for (File f : files) {
                        // 获取文件绝对路径
                        String absolutePath = f.getAbsolutePath();
                        // 判断是否为class文件
                        if (absolutePath.endsWith(".class")) {
                            // 获取类的全限定名
                            String className = absolutePath.substring(absolutePath.lastIndexOf("\\") + 1, absolutePath.lastIndexOf("."));
                            String classPath = String.format("%s.%s", path, className);
                            // 根据全限定名加载类
                            try {
                                Class<?> aClass = classLoader.loadClass(classPath);
                                // 判断是否为bean
                                if (aClass.isAnnotationPresent(Component.class)) {
                                    // 创建bean定义
                                    BeanDefinition beanDefinition = createBeanDefinition(aClass);
                                    // 获取bean名称
                                    Component component = aClass.getAnnotation(Component.class);
                                    // 将bean定义放入map中
                                    if ("".equals(component.value())) {
                                        beanDefinitionMap.put(aClass.getSimpleName(), beanDefinition);
                                    } else {
                                        beanDefinitionMap.put(component.value(), beanDefinition);
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        // 实例化单例bean
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            // 单例的bean则 直接创建bean对象
            if ("singleton".equals(beanDefinition.getScope())) {
                Object bean = createBean(beanName, beanDefinition);
                // 将bean放入单例池中
                singletonMap.put(beanName, bean);
            }
        }
    }

    /**
     * 根据bean名称获取bean定义
     *
     * @param beanName bean名称
     * @return bean定义
     */
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NullPointerException();
        }
        if ("singleton".equals(beanDefinition.getScope())) {
            // 从单例池中获取bean
            Object bean = singletonMap.get(beanName);
            if (bean == null) {
                synchronized (this) {
                    if (bean == null) {
                        Object singletonBean = createBean(beanName, beanDefinition);
                        singletonMap.put(beanName, singletonBean);
                        return singletonBean;
                    }
                }
            }
            return bean;
        }
        // 多例
        return createBean(beanName, beanDefinition);
    }

    /**
     * 创建bean对象
     *
     * @param beanName       bean名称
     * @param beanDefinition bean定义
     * @return bean对象
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> clazz = beanDefinition.getClazz();
        try {
            Object bean = clazz.getConstructor().newInstance();

            return bean;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 创建bean定义
     *
     * @param aClass 类
     * @return bean定义
     */
    private static BeanDefinition createBeanDefinition(Class<?> aClass) {
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setClazz(aClass);
        // 判断是否为单例
        if (aClass.isAnnotationPresent(Scope.class)) {
            Scope scope = aClass.getAnnotation(Scope.class);
            beanDefinition.setScope(scope.value());
        } else {
            beanDefinition.setScope("singleton");
        }
        return beanDefinition;
    }
}
