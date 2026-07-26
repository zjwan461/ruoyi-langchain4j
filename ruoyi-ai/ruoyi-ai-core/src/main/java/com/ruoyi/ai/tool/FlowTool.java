package com.ruoyi.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.spring.SpringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 父 Agent 使用的子 Agent 选择工具。
 * <p>
 * 当前实现只暴露 {@link #selectSubAgents(List, List)} 这一种入口，由它统一负责：
 * 多 Agent 选择、单 Agent 选择、空批校验、上限校验、顺序保留。
 * 调度层读取结果后会严格按返回顺序串行执行，不做并行/插队。
 */
@Slf4j
public class FlowTool {

    /**
     * 给 agent_selector 工具使用的工具描述：
     * 模型一次传入 agentIds 数组，系统会按数组顺序严格串行执行。
     */
    private static final String AGENT_SELECTOR_DESC = "根据选中的子Agent ID数组，按顺序返回选中子智能体的详细信息（JSON数组）。"
            + "调用方会按你返回的数组顺序严格串行调度：前一个 Agent 完成后，下一个 Agent 才会启动，"
            + "并能看到前面 Agent 的输出和原始用户请求。";

    /**
     * agent_selector 工具的 agentIds 参数描述（含上限说明，参考 MAX_FLOW_BATCH_SIZE）。
     */
    private static final String AGENT_IDS_PARAM_DESC = "选中的下一步要顺序串行调用的AgentId列表（顺序就是执行顺序，长度不超过"
            + Constants.MAX_FLOW_BATCH_SIZE + "）";

    /**
     * 多 Agent 选择：模型一次传入一组 agentIds，工具返回按顺序排列的子 Agent 列表（JSON 数组）。
     * <p>
     * 调度层读取结果后会严格按返回顺序串行执行，不做并行/插队；返回数组的顺序就是执行顺序。
     * 行为约束：
     * <ul>
     *     <li>{@code agentIds} 为 null 或空数组：工具返回 {@code "[]"}，调度层解析时会
     *         fail-fast，以满足"校验空列表"的需求。</li>
     *     <li>只有 1 个 id：按长度为 1 的有序数组处理。</li>
     *     <li>任何 agentId 不在 subAgents 列表中或为 null：fail-fast {@link IllegalArgumentException}。</li>
     *     <li>超过 {@link Constants#MAX_FLOW_BATCH_SIZE} 上限：fail-fast。</li>
     * </ul>
     */
    @Tool(value = AGENT_SELECTOR_DESC, name = Constants.AGENT_SELECTOR_TOOL)
    public String selectSubAgents(@P("可选子智能体列表") List<AgentSpec> subAgents,
                                  @P(AGENT_IDS_PARAM_DESC) List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            // 空数组显式返回 []，与 onToolExecuted 中 Array 解析路径保持一致；后续
            // 解析为空 List 时再 fail-fast，以满足"校验空列表"的需求。
            return "[]";
        }
        if (agentIds.size() > Constants.MAX_FLOW_BATCH_SIZE) {
            throw new IllegalArgumentException("agentIds length " + agentIds.size()
                    + " exceeds MAX_FLOW_BATCH_SIZE " + Constants.MAX_FLOW_BATCH_SIZE);
        }

        List<AgentSpec> orderedSpecs = new ArrayList<>(agentIds.size());
        for (Long id : agentIds) {
            if (id == null) {
                throw new IllegalArgumentException("agentIds contains null element");
            }
            Optional<AgentSpec> match = subAgents == null ? Optional.empty() : subAgents.stream()
                    .filter(Objects::nonNull)
                    .filter(spec -> id.equals(spec.getAgentId()))
                    .findFirst();
            if (match.isEmpty()) {
                throw new IllegalArgumentException(
                        "agentId " + id + " is not present in provided subAgents list");
            }
            orderedSpecs.add(match.get());
        }

        ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        try {
            return objectMapper.writeValueAsString(orderedSpecs);
        } catch (JsonProcessingException e) {
            log.error("AgentSpec 列表转json错误", e);
            return null;
        }
    }
}
