package com.ruoyi.ai.service.grpc.server;

import com.ruoyi.ai.domain.AgentMcpService;
import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.service.IAgentMcpService;
import com.ruoyi.ai.service.grpc.impl.GetMcpServiceByIdsRequest;
import com.ruoyi.ai.service.grpc.impl.GetMcpServiceByIdsResponse;
import com.ruoyi.ai.service.grpc.impl.AgentMcpGrpcServiceGrpc.AgentMcpGrpcServiceImplBase;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import java.util.List;

@GrpcService
@Component
@Slf4j
public class AgentMcpGrpcServer extends AgentMcpGrpcServiceImplBase {

    private final IAgentMcpService agentMcpService;

    public AgentMcpGrpcServer(IAgentMcpService agentMcpService) {
        this.agentMcpService = agentMcpService;
    }

    @Override
    public void getMcpServiceByIds(GetMcpServiceByIdsRequest request, StreamObserver<GetMcpServiceByIdsResponse> responseObserver) {
        try {
            List<Long> idsList = request.getIdsList();
            Long[] ids = idsList.toArray(new Long[0]);
            
            List<AgentMcpService> mcpServices = agentMcpService.selectAgentMcpServiceByIds(ids);
            
            GetMcpServiceByIdsResponse.Builder responseBuilder = GetMcpServiceByIdsResponse.newBuilder();
            
            for (AgentMcpService mcpService : mcpServices) {
                com.ruoyi.ai.service.grpc.impl.AgentMcpService.Builder serviceBuilder = 
                    com.ruoyi.ai.service.grpc.impl.AgentMcpService.newBuilder()
                        .setId(mcpService.getId() != null ? mcpService.getId() : 0)
                        .setMcpServiceName(mcpService.getMcpServiceName() != null ? mcpService.getMcpServiceName() : "")
                        .setMcpUrl(mcpService.getMcpUrl() != null ? mcpService.getMcpUrl() : "")
                        .setMcpTransport(mcpService.getMcpTransport() != null ? mcpService.getMcpTransport() : "")
                        .setAuthToken(mcpService.getAuthToken() != null ? mcpService.getAuthToken() : "");
                
                // Convert AgentMcpTool list
                List<AgentMcpTool> mcpTools = mcpService.getAgentMcpToolList();
                if (mcpTools != null && !mcpTools.isEmpty()) {
                    for (AgentMcpTool mcpTool : mcpTools) {
                        com.ruoyi.ai.service.grpc.impl.AgentMcpTool grpcMcpTool = 
                            com.ruoyi.ai.service.grpc.impl.AgentMcpTool.newBuilder()
                                .setId(mcpTool.getId() != null ? mcpTool.getId() : 0)
                                .setMcpToolName(mcpTool.getMcpToolName() != null ? mcpTool.getMcpToolName() : "")
                                .setMcpToolDescription(mcpTool.getMcpToolDescription() != null ? mcpTool.getMcpToolDescription() : "")
                                .setMcpToolParameters(mcpTool.getMcpToolParameters() != null ? mcpTool.getMcpToolParameters() : "")
                                .setMcpToolMetadata(mcpTool.getMcpToolMetadata() != null ? mcpTool.getMcpToolMetadata() : "")
                                .setPid(mcpTool.getPid() != null ? mcpTool.getPid() : 0)
                                .build();
                        serviceBuilder.addAgentMcpToolList(grpcMcpTool);
                    }
                }
                
                responseBuilder.addServices(serviceBuilder.build());
            }
            
            GetMcpServiceByIdsResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("grpc calling error", e);
            responseObserver.onError(e);
        }
    }
}
