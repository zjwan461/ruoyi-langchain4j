package com.ruoyi.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AsyncWebMvcConfig implements WebMvcConfigurer {

  @Autowired
  ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(60 * 1000L);
    configurer.setTaskExecutor(threadPoolTaskExecutor);
  }
}
