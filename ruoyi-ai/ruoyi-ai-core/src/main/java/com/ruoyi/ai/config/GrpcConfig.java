package com.ruoyi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;

@Configuration
public class GrpcConfig {

     /**
     * 基础认证读取器（支持Basic Auth / Header Token扩展）
     */
    @Bean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        // 自带基础Basic认证实现，也可自定义实现Header Token读取
        return new BasicGrpcAuthenticationReader();
    }
    
}
