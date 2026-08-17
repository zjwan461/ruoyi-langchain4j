package com.ruoyi.ai.service.grpc.server;

import com.ruoyi.ai.domain.AgentMcpTool;
import com.ruoyi.ai.domain.AgentSkill;
import com.ruoyi.ai.domain.AgentTool;
import com.ruoyi.ai.domain.AIAgent;
import com.ruoyi.ai.service.IAIAgentService;
import com.ruoyi.ai.service.grpc.impl.GetAiAgentByIdRequest;
import com.ruoyi.ai.service.grpc.impl.GetAiAgentByIdResponse;
import com.ruoyi.ai.service.grpc.impl.AiAgentGrpcServiceGrpc.AiAgentGrpcServiceImplBase;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import java.util.List;

@GrpcService
@Component
@Slf4j
public class AiAgentGrpcServer extends AiAgentGrpcServiceImplBase {

    private final IAIAgentService aiAgentService;

    public AiAgentGrpcServer(IAIAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @Override
    public void getAiAgentById(GetAiAgentByIdRequest request, StreamObserver<GetAiAgentByIdResponse> responseObserver) {
        try {
            Long agentId = request.getAgentId();
            AIAgent agent = aiAgentService.selectAiAgentById(agentId);
            com.ruoyi.ai.service.grpc.impl.AiAgent grpcAgent = convertToGrpcAgent(agent);

            GetAiAgentByIdResponse response = GetAiAgentByIdResponse.newBuilder()
                    .setAgent(grpcAgent)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("grpc calling error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAiAgentWithToolAndSkillById(GetAiAgentByIdRequest request, StreamObserver<GetAiAgentByIdResponse> responseObserver) {
        try {
            Long agentId = request.getAgentId();
            AIAgent agent = aiAgentService.selectAiAgentWithToolAndSkillById(agentId);
            com.ruoyi.ai.service.grpc.impl.AiAgent grpcAgent = convertToGrpcAgent(agent);

            GetAiAgentByIdResponse response = GetAiAgentByIdResponse.newBuilder()
                    .setAgent(grpcAgent)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("grpc calling error", e);
            responseObserver.onError(e);
        }
    }

    private com.ruoyi.ai.service.grpc.impl.AiAgent convertToGrpcAgent(AIAgent agent) {
        com.ruoyi.ai.service.grpc.impl.AiAgent.Builder builder = com.ruoyi.ai.service.grpc.impl.AiAgent.newBuilder()
                .setId(agent.getId() != null ? agent.getId() : 0)
                .setName(agent.getName() != null ? agent.getName() : "")
                .setKbIds(agent.getKbIds() != null ? agent.getKbIds() : "")
                .setKbNames(agent.getKbNames() != null ? agent.getKbNames() : "")
                .setSystemMessage(agent.getSystemMessage() != null ? agent.getSystemMessage() : "")
                .setMemoryCount(agent.getMemoryCount() != null ? agent.getMemoryCount() : 0)
                .setModelId(agent.getModelId() != null ? agent.getModelId() : 0)
                .setModelName(agent.getModelName() != null ? agent.getModelName() : "")
                .setAgentType(agent.getAgentType() != null ? agent.getAgentType() : "")
                .setStatus(agent.getStatus() != null ? agent.getStatus() : 0)
                .setVisitUrl(agent.getVisitUrl() != null ? agent.getVisitUrl() : "")
                .setDifyApiId(agent.getDifyApiId() != null ? agent.getDifyApiId() : 0)
                .setDifyApiName(agent.getDifyApiName() != null ? agent.getDifyApiName() : "")
                .setDayLmtPerClient(agent.getDayLmtPerClient() != null ? agent.getDayLmtPerClient() : 0)
                .setTemperature(agent.getTemperature() != null ? agent.getTemperature() : 0.0)
                .setMaxOutputToken(agent.getMaxOutputToken() != null ? agent.getMaxOutputToken() : 0)
                .setPromptTemplate(agent.getPromptTemplate() != null ? agent.getPromptTemplate() : "")
                .setMinScore(agent.getMinScore() != null ? agent.getMinScore() : 0.0)
                .setMaxResult(agent.getMaxResult() != null ? agent.getMaxResult() : 0)
                .setDescription(agent.getDescription() != null ? agent.getDescription() : "")
                .setToolIds(agent.getToolIds() != null ? agent.getToolIds() : "")
                .setToolNames(agent.getToolNames() != null ? agent.getToolNames() : "")
                .setSkillIds(agent.getSkillIds() != null ? agent.getSkillIds() : "")
                .setSkillNames(agent.getSkillNames() != null ? agent.getSkillNames() : "")
                .setMcpToolIds(agent.getMcpToolIds() != null ? agent.getMcpToolIds() : "")
                .setMcpToolNames(agent.getMcpToolNames() != null ? agent.getMcpToolNames() : "")
                .setShowReasoning(agent.getShowReasoning() != null ? agent.getShowReasoning() : false)
                .setFlowMemory(agent.getFlowMemory() != null ? agent.getFlowMemory() : false)
                .setAgentNodeId(agent.getAgentNodeId() != null ? agent.getAgentNodeId() : "")
                .setIsRootAgent(agent.getIsRootAgent() != null ? agent.getIsRootAgent() : false)
                .setWebSearch(agent.getWebSearch() != null ? agent.getWebSearch() : false)
                .setWebSearchCount(agent.getWebSearchCount() != null ? agent.getWebSearchCount() : 0);

        // Convert AgentTool list
        List<AgentTool> agentTools = agent.getAgentTools();
        if (agentTools != null && !agentTools.isEmpty()) {
            for (AgentTool tool : agentTools) {
                com.ruoyi.ai.service.grpc.impl.AgentTool grpcTool = com.ruoyi.ai.service.grpc.impl.AgentTool.newBuilder()
                        .setId(tool.getId() != null ? tool.getId() : 0)
                        .setToolName(tool.getToolName() != null ? tool.getToolName() : "")
                        .setToolType(tool.getToolType() != null ? tool.getToolType() : "")
                        .setCode(tool.getCode() != null ? tool.getCode() : "")
                        .setClazz(tool.getClazz() != null ? tool.getClazz() : "")
                        .setDescription(tool.getDescription() != null ? tool.getDescription() : "")
                        .setParameters(tool.getParameters() != null ? tool.getParameters() : "")
                        .build();
                builder.addAgentTools(grpcTool);
            }
        }

        // Convert AgentSkill list
        List<AgentSkill> agentSkills = agent.getAgentSkills();
        if (agentSkills != null && !agentSkills.isEmpty()) {
            for (AgentSkill skill : agentSkills) {
                com.ruoyi.ai.service.grpc.impl.AgentSkill grpcSkill = com.ruoyi.ai.service.grpc.impl.AgentSkill.newBuilder()
                        .setId(skill.getId() != null ? skill.getId() : 0)
                        .setSkillName(skill.getSkillName() != null ? skill.getSkillName() : "")
                        .setSkillDescription(skill.getSkillDescription() != null ? skill.getSkillDescription() : "")
                        .setSkillContent(skill.getSkillContent() != null ? skill.getSkillContent() : "")
                        .setSkillType(skill.getSkillType() != null ? skill.getSkillType() : "")
                        .build();
                builder.addAgentSkills(grpcSkill);
            }
        }

        // Convert AgentMcpTool list
        List<AgentMcpTool> agentMcpTools = agent.getAgentMcpTools();
        if (agentMcpTools != null && !agentMcpTools.isEmpty()) {
            for (AgentMcpTool mcpTool : agentMcpTools) {
                com.ruoyi.ai.service.grpc.impl.AgentMcpTool grpcMcpTool = com.ruoyi.ai.service.grpc.impl.AgentMcpTool.newBuilder()
                        .setId(mcpTool.getId() != null ? mcpTool.getId() : 0)
                        .setMcpToolName(mcpTool.getMcpToolName() != null ? mcpTool.getMcpToolName() : "")
                        .setMcpToolDescription(mcpTool.getMcpToolDescription() != null ? mcpTool.getMcpToolDescription() : "")
                        .setMcpToolParameters(mcpTool.getMcpToolParameters() != null ? mcpTool.getMcpToolParameters() : "")
                        .setMcpToolMetadata(mcpTool.getMcpToolMetadata() != null ? mcpTool.getMcpToolMetadata() : "")
                        .setPid(mcpTool.getPid() != null ? mcpTool.getPid() : 0)
                        .build();
                builder.addAgentMcpTools(grpcMcpTool);
            }
        }

        return builder.build();
    }
}
