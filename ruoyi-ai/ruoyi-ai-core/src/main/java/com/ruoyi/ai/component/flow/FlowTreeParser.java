package com.ruoyi.ai.component.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.component.flow.dto.AgentNode;
import com.ruoyi.ai.component.flow.dto.FlowEdge;
import com.ruoyi.ai.component.flow.dto.FlowExport;
import com.ruoyi.ai.component.flow.dto.TreeNode;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FlowTreeParser {
    private final ObjectMapper objectMapper;

    public FlowTreeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 入口：解析并校验唯一根节点
     */
    public TreeNode parseToTree(String jsonStr) {
        FlowExport flowExport = null;
        try {
            flowExport = objectMapper.readValue(jsonStr, FlowExport.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("解析Agent Flow json错误");
        }
        // 执行校验：必须仅有一个根
        validateRootOnlyOne(flowExport);
        // 构建树形结构
        return buildTree(flowExport);
    }

    /**
     * 独立校验方法：校验流程图有且仅有一个根节点（入度=0）
     *
     * @param flowExport 解析后的流程图原始数据
     * @throws IllegalStateException 无根/多根抛出异常
     */
    public void validateRootOnlyOne(FlowExport flowExport) {
        List<AgentNode> allNodes = flowExport.getNodes();
        List<FlowEdge> allEdges = flowExport.getEdges();

        Map<String, Integer> inDegreeMap = new HashMap<>();
        // 初始化所有节点入度0
        for (AgentNode node : allNodes) {
            inDegreeMap.put(node.getNodeId(), 0);
        }
        // 统计每条边目标节点入度
        for (FlowEdge edge : allEdges) {
            String targetId = edge.getTargetNodeId();
            inDegreeMap.put(targetId, inDegreeMap.get(targetId) + 1);
        }
        // 筛选根节点（入度=0）
        List<String> rootNodeIds = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inDegreeMap.entrySet()) {
            if (entry.getValue() == 0) {
                rootNodeIds.add(entry.getKey());
            }
        }
        // 校验规则
        if (rootNodeIds.isEmpty()) {
            throw new IllegalStateException("校验失败：流程图不存在根节点（无入口节点）");
        }
        if (rootNodeIds.size() > 1) {
            throw new IllegalStateException(
                    "校验失败：流程图仅允许1个根节点，当前根节点数量=" + rootNodeIds.size()
                            + "，根节点ID列表：" + rootNodeIds
            );
        }
    }

    /**
     * 内部方法：根据FlowExport构建完整树形结构
     */
    private TreeNode buildTree(FlowExport flowExport) {
        List<AgentNode> allNodes = flowExport.getNodes();
        List<FlowEdge> allEdges = flowExport.getEdges();

        Map<String, AgentNode> nodeMap = new HashMap<>();
        Map<String, Integer> inDegreeMap = new HashMap<>();
        Map<String, List<String>> parentChildMap = new HashMap<>();

        for (AgentNode node : allNodes) {
            String nodeId = node.getNodeId();
            nodeMap.put(nodeId, node);
            inDegreeMap.put(nodeId, 0);
            parentChildMap.put(nodeId, new ArrayList<>());
        }

        // 构建父子映射 & 入度统计
        for (FlowEdge edge : allEdges) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();
            parentChildMap.get(sourceId).add(targetId);
            inDegreeMap.put(targetId, inDegreeMap.get(targetId) + 1);
        }

        // 缓存所有TreeNode
        Map<String, TreeNode> treeNodeMap = new HashMap<>();
        for (Map.Entry<String, AgentNode> entry : nodeMap.entrySet()) {
            treeNodeMap.put(entry.getKey(), new TreeNode(entry.getValue()));
        }

        // 绑定父子关系
        for (Map.Entry<String, List<String>> entry : parentChildMap.entrySet()) {
            TreeNode parent = treeNodeMap.get(entry.getKey());
            for (String childId : entry.getValue()) {
                parent.addChild(treeNodeMap.get(childId));
            }
        }

        // 获取唯一根（前面已校验数量=1）
        for (Map.Entry<String, Integer> entry : inDegreeMap.entrySet()) {
            if (entry.getValue() == 0) {
                return treeNodeMap.get(entry.getKey());
            }
        }
        throw new IllegalStateException("根节点丢失，校验逻辑异常");
    }

    /**
     * 控制台打印整棵树
     */
    public void printTree(TreeNode root) {
        printNode(root, 0);
    }

    private void printNode(TreeNode treeNode, int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }
        AgentNode node = treeNode.getNode();
        System.out.printf("%s[节点ID:%s | 智能体:%s]%n",
                indent, node.getNodeId(), node.getAgentName());
        for (TreeNode child : treeNode.getChildren()) {
            printNode(child, level + 1);
        }
    }

    /**
     * 根据nodeId从整棵树中查找对应TreeNode（对外调用入口）
     * @param root 根节点
     * @param targetNodeId 目标节点nodeId
     * @return 匹配的TreeNode，未找到返回null
     */
    public TreeNode findTreeNodeById(TreeNode root, String targetNodeId) {
        if (root == null || targetNodeId == null || targetNodeId.isBlank()) {
            return null;
        }
        return dfsSearchNode(root, targetNodeId);
    }

    /**
     * 深度优先递归查找节点（私有辅助）
     */
    private TreeNode dfsSearchNode(TreeNode current, String targetNodeId) {
        // 当前节点匹配直接返回
        if (targetNodeId.equals(current.getNode().getNodeId())) {
            return current;
        }
        // 遍历子节点递归查找
        for (TreeNode child : current.getChildren()) {
            TreeNode result = dfsSearchNode(child, targetNodeId);
            if (result != null) {
                return result;
            }
        }
        // 全部子节点无匹配
        return null;
    }
}