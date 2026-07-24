package com.ruoyi.ai.tool;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LRUCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class FlowTool {

    private static final LRUCache<String, String> cache = CacheUtil.newLRUCache(10000);

    @Tool(value = "根据选中子智能体的ID，从可选智能体列表中返回选中子智能体的详细信息。调用完成后需要马上调用" + Constants.AGENT_RESULT_TOOL + "获取结果",
            name = Constants.AGENT_SELECTOR_TOOL)
    public String selectSubAgent(@P("可选子智能体列表") List<AgentSpec> subAgents, @P("选中的下一步要调用的AgentId") Long agentId) {
        Optional<AgentSpec> op = subAgents.stream()
                .filter(spec -> spec.getAgentId().equals(agentId))
                .findFirst();
        AgentSpec agentSpec = op.orElse(null);
        if (agentSpec == null) {
            return null;
        }
        ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        try {
            return objectMapper.writeValueAsString(agentSpec);
        } catch (JsonProcessingException e) {
            log.error("Agent spec转json错误", e);
        }
        return null;
    }
}
