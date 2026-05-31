package com.gym;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class to access Spring ApplicationContext from non-Spring managed classes.
 * This is essential for integrating JavaFX controllers with Spring dependency injection.
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("Spring context not initialized yet");
        }
        return context.getBean(beanClass);
    }

    public static Object getBean(String beanName) {
        if (context == null) {
            throw new IllegalStateException("Spring context not initialized yet");
        }
        return context.getBean(beanName);
    }

    public static boolean isInitialized() {
        return context != null;
    }
}
