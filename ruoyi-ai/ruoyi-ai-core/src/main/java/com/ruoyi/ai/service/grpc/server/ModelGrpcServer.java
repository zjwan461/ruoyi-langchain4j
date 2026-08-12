package com.ruoyi.ai.service.grpc.server;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.service.grpc.impl.ListModelRequest;
import com.ruoyi.ai.service.grpc.impl.ListModelResponse;
import com.ruoyi.ai.service.grpc.impl.ModelGrpcServiceGrpc.ModelGrpcServiceImplBase;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Component
@Slf4j
public class ModelGrpcServer extends ModelGrpcServiceImplBase {

    private final IModelService modelService;

    public ModelGrpcServer(IModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public void listModel(ListModelRequest request, StreamObserver<ListModelResponse> responseObserver) {
        try {
            List<Model> models = modelService.selectModelList(null);
            List<com.ruoyi.ai.service.grpc.impl.Model> respList = models.stream().map(model -> {
                return com.ruoyi.ai.service.grpc.impl.Model.newBuilder()
                        .setId(model.getId())
                        .setName(model.getName())
                        .setBaseUrl(model.getBaseUrl())
                        .setApiKey(model.getApiKey())
                        .setTemperature(model.getTemperature())
                        .setMaxOutputToken(model.getMaxOutputToken())
                        .setType(model.getType())
                        .setProvider(model.getProvider())
                        .build();
            }).toList();

            // 3. 构建返回响应体，填充列表
            ListModelResponse response = ListModelResponse.newBuilder()
                    .addAllModelList(respList)
                    .build();

            // 4. 返回结果给客户端
            responseObserver.onNext(response);
            // 5. 标记本次调用完成（必须调用，否则客户端会一直等待）
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("grpc calling error", e);
            responseObserver.onError(e);
        }

    }

}
