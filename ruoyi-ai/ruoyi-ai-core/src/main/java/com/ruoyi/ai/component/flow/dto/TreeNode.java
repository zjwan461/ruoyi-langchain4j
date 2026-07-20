package com.ruoyi.ai.component.flow.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用树形结构，封装节点父子关系
 */
public class TreeNode {
    // 原始节点数据
    private AgentNode node;
    // 子节点列表
    private List<TreeNode> children;

    public TreeNode() {
        this.children = new ArrayList<>();
    }

    public TreeNode(AgentNode node) {
        this.node = node;
        this.children = new ArrayList<>();
    }

    /**
     * 添加子节点
     */
    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    // getter & setter
    public AgentNode getNode() {
        return node;
    }

    public void setNode(AgentNode node) {
        this.node = node;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
}