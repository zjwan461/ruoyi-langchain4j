package com.ruoyi.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.dto.AgentSpec;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FlowTool} 多 Agent 选择工具的单元测试（无单 Agent 兼容分支）。
 * <p>
 * 目标：
 * <ul>
 *     <li>正常路径：按 agentIds 顺序返回对应 AgentSpec 数组</li>
 *     <li>单元素情形：长度为 1 的数组（agent_selector 没有"单 Agent"特权路径，必须走数组）</li>
 *     <li>空数组 / null agentIds：返回 {@code "[]"}（下游解析时再 fail-fast）</li>
 *     <li>超出 MAX_FLOW_BATCH_SIZE：抛 IllegalArgumentException</li>
 *     <li>agentId 不在 subAgents：抛 IllegalArgumentException</li>
 *     <li>agentIds 含 null：抛 IllegalArgumentException</li>
 *     <li>JSON 反序列化回到 List<AgentSpec> 字段保留顺序</li>
 * </ul>
 */
class FlowToolTest {

    private MockedStatic<SpringUtils> springUtilsMock;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        springUtilsMock = Mockito.mockStatic(SpringUtils.class);
        springUtilsMock.when(() -> SpringUtils.getBean(ObjectMapper.class)).thenReturn(objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (springUtilsMock != null) {
            springUtilsMock.close();
        }
    }

    @Test
    @DisplayName("正常：按 agentIds 入参顺序返回对应 AgentSpec 数组")
    void selectSubAgents_ordersByAgentIds() throws Exception {
        List<AgentSpec> subs = Arrays.asList(
                AgentSpec.builder().agentId(1L).agentName("A").description("a").build(),
                AgentSpec.builder().agentId(2L).agentName("B").description("b").build(),
                AgentSpec.builder().agentId(3L).agentName("C").description("c").build()
        );
        // 倒序传入也应保留倒序（顺序=执行顺序）
        String json = new FlowTool().selectSubAgents(subs, Arrays.asList(3L, 1L, 2L));
        assertNotNull(json);
        List<AgentSpec> back = objectMapper.readValue(json, new TypeReference<List<AgentSpec>>() {
        });
        assertEquals(3, back.size());
        assertEquals(Long.valueOf(3L), back.get(0).getAgentId());
        assertEquals(Long.valueOf(1L), back.get(1).getAgentId());
        assertEquals(Long.valueOf(2L), back.get(2).getAgentId());
        // 名称也应同步保留
        assertEquals("C", back.get(0).getAgentName());
    }

    @Test
    @DisplayName("单元素：长度为 1 的数组（agent_selector 统一走数组，无单 Agent 特权分支）")
    void selectSubAgents_singleElementArrayOnly() throws Exception {
        AgentSpec only = AgentSpec.builder().agentId(42L).agentName("Solo").build();
        String json = new FlowTool().selectSubAgents(Collections.singletonList(only), Collections.singletonList(42L));
        List<AgentSpec> back = objectMapper.readValue(json, new TypeReference<List<AgentSpec>>() {
        });
        assertEquals(1, back.size());
        assertEquals(Long.valueOf(42L), back.get(0).getAgentId());
        assertEquals("Solo", back.get(0).getAgentName());
    }

    @Test
    @DisplayName("空 agentIds：返回 [] 不抛异常")
    void selectSubAgents_emptyReturnsEmptyArray() {
        String json = new FlowTool().selectSubAgents(Collections.emptyList(), Collections.emptyList());
        assertEquals("[]", json);
    }

    @Test
    @DisplayName("null agentIds：返回 [] 不抛异常")
    void selectSubAgents_nullReturnsEmptyArray() {
        String json = new FlowTool().selectSubAgents(Collections.emptyList(), null);
        assertEquals("[]", json);
    }

    @Test
    @DisplayName("超过 MAX_FLOW_BATCH_SIZE 上限：fail-fast IllegalArgumentException")
    void selectSubAgents_overLimitFailsFast() {
        // 构造超过上限的 agentIds
        int overLimit = Constants.MAX_FLOW_BATCH_SIZE + 1;
        List<Long> tooMany = new java.util.ArrayList<>(overLimit);
        List<AgentSpec> subs = new java.util.ArrayList<>(overLimit);
        for (long i = 1; i <= overLimit; i++) {
            tooMany.add(i);
            subs.add(AgentSpec.builder().agentId(i).agentName("A" + i).build());
        }
        FlowTool tool = new FlowTool();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool.selectSubAgents(subs, tooMany));
        assertTrue(ex.getMessage().contains("MAX_FLOW_BATCH_SIZE"),
                "应当提示超过 MAX_FLOW_BATCH_SIZE 上限");
    }

    @Test
    @DisplayName("agentId 不在 subAgents 列表中：fail-fast IllegalArgumentException")
    void selectSubAgents_unknownAgentIdFailsFast() {
        List<AgentSpec> subs = Collections.singletonList(
                AgentSpec.builder().agentId(1L).agentName("A").build());
        FlowTool tool = new FlowTool();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool.selectSubAgents(subs, Collections.singletonList(99L)));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("agentIds 包含 null：fail-fast IllegalArgumentException")
    void selectSubAgents_nullElementFailsFast() {
        AgentSpec a = AgentSpec.builder().agentId(1L).agentName("A").build();
        List<AgentSpec> subs = Collections.singletonList(a);
        FlowTool tool = new FlowTool();
        assertThrows(IllegalArgumentException.class,
                () -> tool.selectSubAgents(subs, Collections.singletonList(null)));
    }

    @Test
    @DisplayName("subAgents 为 null 时，仅校验 null id（不会 NPE）")
    void selectSubAgents_subAgentsNullTolerant() {
        FlowTool tool = new FlowTool();
        assertThrows(IllegalArgumentException.class,
                () -> tool.selectSubAgents(null, Collections.singletonList(1L)));
    }
}
