package com.ruoyi.ai.grpc;

import com.ruoyi.ai.service.grpc.impl.ListModelRequest;
import com.ruoyi.ai.service.grpc.impl.ListModelResponse;
import com.ruoyi.ai.service.grpc.impl.ModelGrpcServiceGrpc;
import com.ruoyi.ai.service.grpc.impl.ModelGrpcServiceGrpc.ModelGrpcServiceBlockingStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ModelGrpcServerTest {

    public static void main(String[] args) {
        // 1. 创建通道，连接grpc服务地址（默认端口9090）
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 9090)
                .usePlaintext() // 本地测试关闭tls，生产环境需要证书
                .build();

        // 2. 创建阻塞Stub（同步调用，最简单）
        ModelGrpcServiceBlockingStub stub = ModelGrpcServiceGrpc.newBlockingStub(channel);

        // 3. 空请求
        ListModelRequest request = ListModelRequest.newBuilder().build();

        // 4. 同步调用接口
        ListModelResponse response = stub.listModel(request);

        // 5. 遍历返回的模型列表
        response.getModelListList().forEach(model -> {
            System.out.println("模型ID：" + model.getId());
            System.out.println("模型名称：" + model.getName());
        });

        channel.shutdown();
    }
}
