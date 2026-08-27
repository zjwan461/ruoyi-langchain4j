package com.ruoyi.framework.web.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.anji.captcha.service.CaptchaCacheService;

/**
 * 自定义redis验证码缓存实现类
 * 
 * @author ruoyi
 */
public class CaptchaRedisService implements CaptchaCacheService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Long increment(String key, long val) {
        return stringRedisTemplate.opsForValue().increment(key, val);
    }

    @Override
    public String type() {
        return "redis";
    }
}