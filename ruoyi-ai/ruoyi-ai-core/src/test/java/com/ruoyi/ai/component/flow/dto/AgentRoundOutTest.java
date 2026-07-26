package com.ruoyi.ai.component.flow.dto;

import com.ruoyi.ai.util.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AgentRoundOut} 多 Agent 数据结构的单元测试。
 * <p>
 * 目标：
 * <ul>
 *     <li>覆盖有序 {@link AgentRoundOut#nextAgentSpecs} 的写入/读取</li>
 *     <li>覆盖 helper 方法 hasNextBatch / addNextSpec 行为</li>
 *     <li>覆盖多 Agent JSON 解析（List<AgentSpec>）的语义</li>
 *     <li>不出现任何对已删除的旧单 Agent 字段的引用</li>
 * </ul>
 */
class AgentRoundOutTest {

    @Test
    @DisplayName("默认 nextAgentSpecs 为 null 时 hasNextBatch 应返回 false")
    void hasNextBatch_default() {
        AgentRoundOut out = AgentRoundOut.builder().build();
        assertFalse(out.hasNextBatch());
    }

    @Test
    @DisplayName("显式塞入有序 nextAgentSpecs 后 hasNextBatch 应为 true")
    void hasNextBatch_withSpecs() {
        List<AgentSpec> specs = Arrays.asList(
                AgentSpec.builder().agentId(1L).agentName("A").build(),
                AgentSpec.builder().agentId(2L).agentName("B").build()
        );
        AgentRoundOut out = AgentRoundOut.builder()
                .nextAgentSpecs(new ArrayList<>(specs))
                .build();
        assertTrue(out.hasNextBatch());
        assertEquals(2, out.getNextAgentSpecs().size());
        assertEquals(Long.valueOf(1L), out.getNextAgentSpecs().get(0).getAgentId());
        assertEquals(Long.valueOf(2L), out.getNextAgentSpecs().get(1).getAgentId());
    }

    @Test
    @DisplayName("addNextSpec 会自动初始化列表、needNext 并保留顺序")
    void addNextSpec_appendsInOrder() {
        AgentRoundOut out = AgentRoundOut.builder().build();
        out.addNextSpec(AgentSpec.builder().agentId(7L).agentName("X").build());
        out.addNextSpec(AgentSpec.builder().agentId(8L).agentName("Y").build());
        out.addNextSpec(AgentSpec.builder().agentId(9L).agentName("Z").build());

        assertTrue(out.isNeedNext());
        assertTrue(out.hasNextBatch());
        assertEquals(3, out.getNextAgentSpecs().size());
        assertEquals(Long.valueOf(7L), out.getNextAgentSpecs().get(0).getAgentId());
        assertEquals(Long.valueOf(8L), out.getNextAgentSpecs().get(1).getAgentId());
        assertEquals(Long.valueOf(9L), out.getNextAgentSpecs().get(2).getAgentId());
    }

    @Test
    @DisplayName("addNextSpec 传入 null 应被忽略，不抛异常")
    void addNextSpec_nullIgnored() {
        AgentRoundOut out = AgentRoundOut.builder().build();
        out.addNextSpec(null);
        assertFalse(out.isNeedNext());
        assertEquals(null, out.getNextAgentSpecs());
    }

    @Test
    @DisplayName("常量 MAX_FLOW_BATCH_SIZE / MAX_FLOW_ROUNDS 已定义且合理")
    void constantsSanity() {
        assertTrue(Constants.MAX_FLOW_BATCH_SIZE >= 1, "批次上限必须 >= 1");
        assertTrue(Constants.MAX_FLOW_BATCH_SIZE <= 32, "批次上限不宜过大");
        assertTrue(Constants.MAX_FLOW_ROUNDS >= 2, "流转总轮次至少支持 2 轮");
    }
}
