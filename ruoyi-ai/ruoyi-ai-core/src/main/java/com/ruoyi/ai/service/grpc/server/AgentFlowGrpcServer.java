package com.ruoyi.ai.service.grpc.server;

import com.ruoyi.ai.domain.AgentFlow;
import com.ruoyi.ai.service.IAgentFlowService;
import com.ruoyi.ai.service.grpc.impl.GetAgentFlowByIdRequest;
import com.ruoyi.ai.service.grpc.impl.GetAgentFlowByIdResponse;
import com.ruoyi.ai.service.grpc.impl.AgentFlowGrpcServiceGrpc.AgentFlowGrpcServiceImplBase;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

@GrpcService
@Component
@Slf4j
public class AgentFlowGrpcServer extends AgentFlowGrpcServiceImplBase {

    private final IAgentFlowService agentFlowService;

    public AgentFlowGrpcServer(IAgentFlowService agentFlowService) {
        this.agentFlowService = agentFlowService;
    }

    @Override
    public void getAgentFlowById(GetAgentFlowByIdRequest request, StreamObserver<GetAgentFlowByIdResponse> responseObserver) {
        try {
            Long agentFlowId = request.getAgentFlowId();
            AgentFlow agentFlow = agentFlowService.selectAgentFlowById(agentFlowId);

            com.ruoyi.ai.service.grpc.impl.AgentFlow grpcAgentFlow = com.ruoyi.ai.service.grpc.impl.AgentFlow.newBuilder()
                    .setId(agentFlow.getId())
                    .setAgentFlowName(agentFlow.getAgentFlowName())
                    .setFlowJson(agentFlow.getFlowJson())
                    .setStatus(agentFlow.getStatus())
                    .setDayLmtPerClient(agentFlow.getDayLmtPerClient())
                    .setVisitUrl(agentFlow.getVisitUrl())
                    .setImplType(agentFlow.getImplType())
                    .build();

            GetAgentFlowByIdResponse response = GetAgentFlowByIdResponse.newBuilder()
                    .setAgentFlow(grpcAgentFlow)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("grpc calling error", e);
            responseObserver.onError(e);
        }
    }
}
