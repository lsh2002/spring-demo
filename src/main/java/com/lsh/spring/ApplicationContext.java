package com.lsh.spring;

import com.lsh.spring.annotation.Autowired;
import com.lsh.spring.annotation.Component;
import com.lsh.spring.annotation.ComponentScan;
import com.lsh.spring.annotation.Scope;
import com.lsh.spring.bean.BeanDefinition;
import com.lsh.spring.bean.BeanNameAware;
import com.lsh.spring.bean.BeanPostProcessor;
import com.lsh.spring.bean.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lsh
 * @version 1.0.0
 * @since  2024/9/26 13:56
 **/
public class ApplicationContext {

    private Class<?> config;

    /**
     * bean定义map
     */
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 单例池
     */
    private ConcurrentHashMap<String, Object> singletonMap = new ConcurrentHashMap<>();

    /**
     * 后处理器
     */
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ApplicationContext(Class<?> configClass) throws URISyntaxException {
        this.config = configClass;

        // 扫描
        // 获取扫描路径
        if (config.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = config.getAnnotation(ComponentScan.class);
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
                                // 判断是否实现了BeanPostProcessor
                                if (BeanPostProcessor.class.isAssignableFrom(aClass)) {
                                    // 实例化后放入集合
                                    beanPostProcessorList.add((BeanPostProcessor) aClass.getDeclaredConstructor().newInstance());
                                }
                                // 判断是否为bean
                                if (aClass.isAnnotationPresent(Component.class)) {
                                    // 创建bean定义
                                    BeanDefinition beanDefinition = createBeanDefinition(aClass);
                                    Component component = aClass.getAnnotation(Component.class);
                                    String beanName = component.value();
                                    // 如果为空则使用类名作为bean名称
                                    if ("".equals(component.value())) {
                                        beanName = Introspector.decapitalize(aClass.getSimpleName());
                                    }
                                    // 将bean定义放入map中
                                    beanDefinitionMap.put(beanName, beanDefinition);
                                }
                            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            } catch (InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        // 实例化单例bean
        beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            // 单例的bean则 直接创建bean对象
            if ("singleton".equals(beanDefinition.getScope())) {
                Object bean = createBean(beanName, beanDefinition);
                // 将bean放入单例池中
                singletonMap.put(beanName, bean);
            }
        });
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
                bean = createBean(beanName, beanDefinition);
                singletonMap.put(beanName, bean);
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
            Object instance = clazz.getConstructor().newInstance();
            // 依赖注入
            Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    declaredField.setAccessible(true);
                    String injectBeanName = declaredField.getName();
                    // 不存在该bean
                    if (!beanDefinitionMap.contains(injectBeanName)) {
                        // 找到实现类
                        for (BeanDefinition value : beanDefinitionMap.values()) {
                            if (declaredField.getType().isAssignableFrom(value.getClazz())) {
                                injectBeanName = Introspector.decapitalize(value.getClazz().getSimpleName());
                            }
                        }
                    }
                    Object injectBean = getBean(injectBeanName);
                    declaredField.set(instance, injectBean);
                }
            }
            // Aware
            if (instance instanceof BeanNameAware beanNameAware) {
                beanNameAware.setBeanName(beanName);
            }
            // 初始化前
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }
            // 初始化
            if (instance instanceof InitializingBean initializingBean) {
                initializingBean.afterPropertiesSet();
            }
            // 初始化后
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }



            return instance;
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
