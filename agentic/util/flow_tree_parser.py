"""
flow_tree_parser.py - 解析 agent_flow_json 结构数据，构建节点树形结构。
参考 Java 实现: com.ruoyi.ai.component.flow.FlowTreeParser
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from functools import lru_cache
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)


# ──────────────────────────── DTO 数据类 ────────────────────────────


@dataclass
class Position:
    """画布节点坐标"""

    x: Optional[int] = None
    y: Optional[int] = None

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> Optional["Position"]:
        if data is None:
            return None
        return cls(x=data.get("x"), y=data.get("y"))


@dataclass
class AgentNodeData:
    """agent-node 的业务数据，对应 X6 节点的 data 字段"""

    agent_id: Optional[int] = None
    name: Optional[str] = None
    description: Optional[str] = None
    show_reasoning: Optional[bool] = None
    flow_memory: Optional[bool] = None

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> Optional["AgentNodeData"]:
        if data is None:
            return None
        return cls(
            agent_id=data.get("agentId"),
            name=data.get("name"),
            description=data.get("description"),
            show_reasoning=data.get("showReasoning"),
            flow_memory=data.get("flowMemory"),
        )


@dataclass
class FlowCell:
    """X6 画布中的通用单元格基类"""

    id: Optional[str] = None
    shape: Optional[str] = None
    z_index: Optional[int] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FlowCell":
        return cls(
            id=data.get("id"),
            shape=data.get("shape"),
            z_index=data.get("zIndex"),
        )


@dataclass
class EdgeEndpoint:
    """X6 连线的 source/target 端点"""

    cell: Optional[str] = None
    port: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> Optional["EdgeEndpoint"]:
        if data is None:
            return None
        return cls(cell=data.get("cell"), port=data.get("port"))


@dataclass
class AgentNode(FlowCell):
    """X6 智能体节点"""

    position: Optional[Position] = None
    visible: Optional[bool] = None
    data: Optional[AgentNodeData] = None

    @property
    def node_id(self) -> Optional[str]:
        return self.id

    @property
    def agent_id(self) -> Optional[int]:
        return self.data.agent_id if self.data else None

    @property
    def agent_name(self) -> Optional[str]:
        return self.data.name if self.data else None

    @property
    def description(self) -> Optional[str]:
        return self.data.description if self.data else None

    @property
    def show_reasoning(self) -> Optional[bool]:
        return self.data.show_reasoning if self.data else None

    @property
    def flow_memory(self) -> Optional[bool]:
        return self.data.flow_memory if self.data else None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentNode":
        return cls(
            id=data.get("id"),
            shape=data.get("shape"),
            z_index=data.get("zIndex"),
            position=Position.from_dict(data.get("position")),
            visible=data.get("visible"),
            data=AgentNodeData.from_dict(data.get("data")),
        )


@dataclass
class FlowEdge(FlowCell):
    """X6 流程连线，起止节点 ID 分别位于 source.cell 和 target.cell"""

    source: Optional[EdgeEndpoint] = None
    target: Optional[EdgeEndpoint] = None

    @property
    def edge_id(self) -> Optional[str]:
        return self.id

    @property
    def source_node_id(self) -> Optional[str]:
        return self.source.cell if self.source else None

    @property
    def target_node_id(self) -> Optional[str]:
        return self.target.cell if self.target else None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FlowEdge":
        return cls(
            id=data.get("id"),
            shape=data.get("shape"),
            z_index=data.get("zIndex"),
            source=EdgeEndpoint.from_dict(data.get("source")),
            target=EdgeEndpoint.from_dict(data.get("target")),
        )


@dataclass
class FlowExport:
    """X6 graph.toJSON() 的导出结构"""

    cells: List[FlowCell] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FlowExport":
        raw_cells: List[Dict[str, Any]] = data.get("cells", [])
        cells: List[FlowCell] = []
        for cell_data in raw_cells:
            shape = cell_data.get("shape")
            if shape == "agent-node":
                cells.append(AgentNode.from_dict(cell_data))
            elif shape == "edge":
                cells.append(FlowEdge.from_dict(cell_data))
            else:
                cells.append(FlowCell.from_dict(cell_data))
        return cls(cells=cells)

    @property
    def nodes(self) -> List[AgentNode]:
        return [c for c in self.cells if isinstance(c, AgentNode)]

    @property
    def edges(self) -> List[FlowEdge]:
        return [c for c in self.cells if isinstance(c, FlowEdge)]


# ──────────────────────────── TreeNode ────────────────────────────


@dataclass
class TreeNode:
    """通用树形结构，封装节点父子关系"""

    node: AgentNode
    children: List["TreeNode"] = field(default_factory=list)

    def add_child(self, child: "TreeNode") -> None:
        self.children.append(child)


# ──────────────────────────── FlowTreeParser ────────────────────────────


class FlowTreeParseError(Exception):
    """流程树解析异常"""

    pass


@lru_cache(maxsize=128)
def _parse_to_tree_cached(json_str: str) -> TreeNode:
    """
    模块级缓存函数：解析 JSON 字符串并构建 TreeNode。
    
    使用 lru_cache 缓存，相同 json_str 不会重复解析。
    
    :param json_str: agent_flow 的 JSON 字符串
    :return: 树的根 TreeNode
    """
    try:
        raw_data = json.loads(json_str)
    except (json.JSONDecodeError, TypeError) as e:
        logger.error("解析 Agent Flow JSON 失败: %s", e)
        raise FlowTreeParseError("解析 Agent Flow json 错误") from e

    flow_export = FlowExport.from_dict(raw_data)
    
    # 校验唯一根节点
    all_nodes = flow_export.nodes
    all_edges = flow_export.edges

    if not all_nodes:
        raise ValueError("校验失败：流程图中不存在智能体节点")

    in_degree_map: Dict[str, int] = {}
    node_ids: set[str] = set()

    for node in all_nodes:
        node_id = node.node_id
        if not node_id:
            raise ValueError("校验失败：智能体节点缺少id")
        if node_id in node_ids:
            raise ValueError(f"校验失败：存在重复的节点ID：{node_id}")
        node_ids.add(node_id)
        if node.agent_id is None:
            raise ValueError(f"校验失败：节点{node_id}的data.agentId不能为空")
        in_degree_map[node_id] = 0

    for edge in all_edges:
        source_id = edge.source_node_id
        target_id = edge.target_node_id
        if source_id not in node_ids or target_id not in node_ids:
            raise ValueError(
                f"校验失败：连线{edge.edge_id}引用了不存在的节点，"
                f"source={source_id}，target={target_id}"
            )
        in_degree_map[target_id] = in_degree_map[target_id] + 1

    root_node_ids = [nid for nid, deg in in_degree_map.items() if deg == 0]

    if not root_node_ids:
        raise ValueError("校验失败：流程图不存在根节点（无入口节点）")
    if len(root_node_ids) > 1:
        raise ValueError(
            f"校验失败：流程图仅允许1个根节点，当前根节点数量={len(root_node_ids)}，"
            f"根节点ID列表：{root_node_ids}"
        )
    
    # 构建树形结构
    node_map: Dict[str, AgentNode] = {}
    parent_child_map: Dict[str, List[str]] = {}

    for node in all_nodes:
        node_id = node.node_id
        node_map[node_id] = node
        parent_child_map[node_id] = []

    # 构建父子映射
    for edge in all_edges:
        source_id = edge.source_node_id
        target_id = edge.target_node_id
        parent_child_map[source_id].append(target_id)

    # 缓存所有 TreeNode
    tree_node_map: Dict[str, TreeNode] = {
        nid: TreeNode(node) for nid, node in node_map.items()
    }

    # 绑定父子关系
    for parent_id, child_ids in parent_child_map.items():
        parent = tree_node_map[parent_id]
        for child_id in child_ids:
            parent.add_child(tree_node_map[child_id])

    # 获取唯一根
    for nid, deg in in_degree_map.items():
        if deg == 0:
            return tree_node_map[nid]

    raise ValueError("根节点丢失，校验逻辑异常")


class FlowTreeParser:
    """
    解析 agent_flow_json 结构数据，校验并构建树形结构。
    对应 Java 实现: com.ruoyi.ai.component.flow.FlowTreeParser
    """

    def parse_to_tree(self, json_str: str) -> TreeNode:
        """
        入口：解析 JSON 字符串并校验唯一根节点，返回树形结构根节点。
        
        内部调用带 LRU 缓存的 _parse_to_tree_cached 函数，相同 json_str 不会重复解析。

        :param json_str: agent_flow 的 JSON 字符串
        :return: 树的根 TreeNode
        :raises FlowTreeParseError: JSON 解析失败
        :raises ValueError: 结构校验失败
        """
        return _parse_to_tree_cached(json_str)

    def validate_root_only_one(self, flow_export: FlowExport) -> None:
        """
        校验流程图有且仅有一个根节点（入度=0）。

        :param flow_export: 解析后的流程图原始数据
        :raises ValueError: 无根 / 多根 / 结构异常时抛出
        """
        all_nodes = flow_export.nodes
        all_edges = flow_export.edges

        if not all_nodes:
            raise ValueError("校验失败：流程图中不存在智能体节点")

        in_degree_map: Dict[str, int] = {}
        node_ids: set[str] = set()

        for node in all_nodes:
            node_id = node.node_id
            if not node_id:
                raise ValueError("校验失败：智能体节点缺少id")
            if node_id in node_ids:
                raise ValueError(f"校验失败：存在重复的节点ID：{node_id}")
            node_ids.add(node_id)
            if node.agent_id is None:
                raise ValueError(f"校验失败：节点{node_id}的data.agentId不能为空")
            in_degree_map[node_id] = 0

        for edge in all_edges:
            source_id = edge.source_node_id
            target_id = edge.target_node_id
            if source_id not in node_ids or target_id not in node_ids:
                raise ValueError(
                    f"校验失败：连线{edge.edge_id}引用了不存在的节点，"
                    f"source={source_id}，target={target_id}"
                )
            in_degree_map[target_id] = in_degree_map[target_id] + 1

        root_node_ids = [nid for nid, deg in in_degree_map.items() if deg == 0]

        if not root_node_ids:
            raise ValueError("校验失败：流程图不存在根节点（无入口节点）")
        if len(root_node_ids) > 1:
            raise ValueError(
                f"校验失败：流程图仅允许1个根节点，当前根节点数量={len(root_node_ids)}，"
                f"根节点ID列表：{root_node_ids}"
            )

    def _build_tree(self, flow_export: FlowExport) -> TreeNode:
        """根据 FlowExport 构建完整树形结构"""
        all_nodes = flow_export.nodes
        all_edges = flow_export.edges

        node_map: Dict[str, AgentNode] = {}
        in_degree_map: Dict[str, int] = {}
        parent_child_map: Dict[str, List[str]] = {}

        for node in all_nodes:
            node_id = node.node_id
            node_map[node_id] = node
            in_degree_map[node_id] = 0
            parent_child_map[node_id] = []

        # 构建父子映射 & 入度统计
        for edge in all_edges:
            source_id = edge.source_node_id
            target_id = edge.target_node_id
            parent_child_map[source_id].append(target_id)
            in_degree_map[target_id] = in_degree_map[target_id] + 1

        # 缓存所有 TreeNode
        tree_node_map: Dict[str, TreeNode] = {
            nid: TreeNode(node) for nid, node in node_map.items()
        }

        # 绑定父子关系
        for parent_id, child_ids in parent_child_map.items():
            parent = tree_node_map[parent_id]
            for child_id in child_ids:
                parent.add_child(tree_node_map[child_id])

        # 获取唯一根
        for nid, deg in in_degree_map.items():
            if deg == 0:
                return tree_node_map[nid]

        raise ValueError("根节点丢失，校验逻辑异常")

    # ──────────── 辅助方法 ────────────

    def print_tree(self, root: TreeNode) -> None:
        """控制台打印整棵树"""
        self._print_node(root, 0)

    def _print_node(self, tree_node: TreeNode, level: int) -> None:
        indent = "  " * level
        node = tree_node.node
        print(f"{indent}[节点ID:{node.node_id} | 智能体:{node.agent_name}]")
        for child in tree_node.children:
            self._print_node(child, level + 1)

    def find_tree_node_by_id(
        self, root: TreeNode, target_node_id: str
    ) -> Optional[TreeNode]:
        """
        根据 node_id 从整棵树中查找对应 TreeNode。

        :param root: 根节点
        :param target_node_id: 目标节点 node_id
        :return: 匹配的 TreeNode，未找到返回 None
        """
        if root is None or not target_node_id:
            return None
        return self._dfs_search_node(root, target_node_id)

    def _dfs_search_node(
        self, current: TreeNode, target_node_id: str
    ) -> Optional[TreeNode]:
        """深度优先递归查找节点"""
        if target_node_id == current.node.node_id:
            return current
        for child in current.children:
            result = self._dfs_search_node(child, target_node_id)
            if result is not None:
                return result
        return None


parser = FlowTreeParser()

if __name__ == "__main__":
    flow_json = """
{"cells":[{"position":{"x":150,"y":130},"size":{"width":160,"height":80},"attrs":{"text":{"text":"Jarvis"}},"visible":true,"shape":"agent-node","ports":{"groups":{"left":{"position":"left","single":false,"attrs":{"circle":{"r":7,"magnet":true,"stroke":"#5F95FF","strokeWidth":2,"fill":"#FFFFFF","cursor":"crosshair"}}},"right":{"position":"right","single":false,"attrs":{"circle":{"r":7,"magnet":true,"stroke":"#5F95FF","strokeWidth":2,"fill":"#FFFFFF","cursor":"crosshair"}}}},"items":[{"id":"left-port","group":"left"},{"id":"right-port","group":"right"}]},"id":"530e18e7-b736-4123-b56b-78285d1029fd","data":{"agentId":1,"name":"Jarvis","description":"生活小管家","showReasoning":true,"flowMemory":true},"zIndex":1},{"position":{"x":427,"y":130},"size":{"width":160,"height":80},"attrs":{"text":{"text":"写作专家"},"label":{"text":"写作专家"}},"visible":true,"shape":"agent-node","ports":{"groups":{"left":{"position":"left","single":false,"attrs":{"circle":{"r":7,"magnet":true,"stroke":"#5F95FF","strokeWidth":2,"fill":"#FFFFFF","cursor":"crosshair"}}},"right":{"position":"right","single":false,"attrs":{"circle":{"r":7,"magnet":true,"stroke":"#5F95FF","strokeWidth":2,"fill":"#FFFFFF","cursor":"crosshair"}}}},"items":[{"id":"left-port","group":"left"},{"id":"right-port","group":"right"}]},"id":"d2d128e5-07f3-4fd1-91c6-6f15aaefd64b","data":{"agentId":2,"name":"写作专家","description":"专门负责各类文章的写作","showReasoning":true,"flowMemory":false},"zIndex":2},{"shape":"edge","id":"1d324856-1782-428d-bb1d-1a3acc14362f","zIndex":3,"source":{"cell":"530e18e7-b736-4123-b56b-78285d1029fd","port":"right-port"},"target":{"cell":"d2d128e5-07f3-4fd1-91c6-6f15aaefd64b","port":"left-port"}}]}
"""
    tree_node: TreeNode = parser.parse_to_tree(flow_json)
    print(tree_node)
