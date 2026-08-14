"""
gRPC 客户端封装模块
用于调用 Java 后端的 gRPC 服务（ModelGrpcService、AiAgentGrpcService）

所有 gRPC 服务共享同一个 Channel（HTTP/2 连接），避免重复建立连接。
"""
import sys
import os
import logging
from typing import List, Dict, Any, Optional
from contextlib import contextmanager

import grpc

# 添加 grpc_generated 目录到 Python 路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'grpc_generated'))

import model_service_pb2
import model_service_pb2_grpc
import ai_agent_service_pb2
import ai_agent_service_pb2_grpc

logger = logging.getLogger(__name__)


class GrpcClient:
    """
    统一 gRPC 客户端，共享同一个 Channel，管理多个 Stub。
    
    支持的 gRPC 服务：
    - ModelGrpcService：模型服务
    - AiAgentGrpcService：AI Agent 服务
    """
    
    def __init__(self, host: str = "127.0.0.1", port: int = 9090):
        """
        初始化 gRPC 客户端
        
        Args:
            host: gRPC 服务器地址，默认 localhost
            port: gRPC 服务器端口，默认 9090
        """
        self.host = host
        self.port = port
        self.channel: Optional[grpc.Channel] = None
        # 多个 Stub 共享同一个 Channel
        self.model_stub: Optional[model_service_pb2_grpc.ModelGrpcServiceStub] = None
        self.ai_agent_stub: Optional[ai_agent_service_pb2_grpc.AiAgentGrpcServiceStub] = None
        
    def connect(self) -> None:
        """建立 gRPC 连接，所有 Stub 共享同一个 Channel"""
        if self.channel is None:
            target = f"{self.host}:{self.port}"
            self.channel = grpc.insecure_channel(target)
            self.model_stub = model_service_pb2_grpc.ModelGrpcServiceStub(self.channel)
            self.ai_agent_stub = ai_agent_service_pb2_grpc.AiAgentGrpcServiceStub(self.channel)
            logger.info(f"已连接到 gRPC 服务器: {target}")
    
    def close(self) -> None:
        """关闭 gRPC 连接"""
        if self.channel is not None:
            self.channel.close()
            self.channel = None
            self.model_stub = None
            self.ai_agent_stub = None
            logger.info("已关闭 gRPC 连接")
    
    def __enter__(self):
        """支持 with 语句"""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """退出 with 语句时自动关闭连接"""
        self.close()
    
    # ============ ModelGrpcService 接口 ============
    
    def list_models(self, timeout: float = 10.0) -> List[Dict[str, Any]]:
        """
        查询模型列表
        
        Args:
            timeout: 请求超时时间（秒），默认 10 秒
            
        Returns:
            模型列表，每个模型是一个字典
            
        Raises:
            grpc.RpcError: gRPC 调用失败时抛出
            RuntimeError: 客户端未连接时抛出
        """
        if self.model_stub is None:
            raise RuntimeError("gRPC 客户端未连接，请先调用 connect() 方法")
        
        try:
            request = model_service_pb2.ListModelRequest()
            response = self.model_stub.ListModel(request, timeout=timeout)
            
            models = []
            for model in response.modelList:
                models.append({
                    "id": model.id,
                    "name": model.name,
                    "baseUrl": model.baseUrl,
                    "apiKey": model.apiKey,
                    "temperature": model.temperature,
                    "maxOutputToken": model.maxOutputToken,
                    "type": model.type,
                    "provider": model.provider,
                })
            
            logger.info(f"成功获取 {len(models)} 个模型")
            return models
            
        except grpc.RpcError as e:
            logger.error(f"gRPC 调用失败: {e.code()} - {e.details()}")
            raise
        except Exception as e:
            logger.error(f"调用模型列表时发生错误: {e}")
            raise
    
    def get_model_by_id(self, model_id: int, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
        """
        根据ID查询模型信息
        
        Args:
            model_id: 模型 ID
            timeout: 请求超时时间（秒），默认 10 秒
            
        Returns:
            模型信息字典，如果不存在则返回 None
            
        Raises:
            grpc.RpcError: gRPC 调用失败时抛出
            RuntimeError: 客户端未连接时抛出
        """
        if self.model_stub is None:
            raise RuntimeError("gRPC 客户端未连接，请先调用 connect() 方法")
        
        try:
            request = model_service_pb2.GetModelByIdRequest(modelId=model_id)
            response = self.model_stub.GetModelById(request, timeout=timeout)
            
            if response.HasField("model"):
                model = response.model
                model_dict = {
                    "id": model.id,
                    "name": model.name,
                    "baseUrl": model.baseUrl,
                    "apiKey": model.apiKey,
                    "temperature": model.temperature,
                    "maxOutputToken": model.maxOutputToken,
                    "type": model.type,
                    "provider": model.provider,
                }
                logger.info(f"成功获取模型 (ID: {model_id}): {model_dict.get('name', 'Unknown')}")
                return model_dict
            else:
                logger.warning(f"未找到 ID 为 {model_id} 的模型")
                return None
                
        except grpc.RpcError as e:
            logger.error(f"gRPC 调用失败: {e.code()} - {e.details()}")
            raise
        except Exception as e:
            logger.error(f"查询模型时发生错误: {e}")
            raise
    
    # ============ AiAgentGrpcService 接口 ============
    
    def _convert_agent_tool_to_dict(self, tool) -> Dict[str, Any]:
        """将 AgentTool protobuf 对象转换为 Python 字典"""
        return {
            "id": tool.id,
            "toolName": tool.toolName,
            "toolType": tool.toolType,
            "code": tool.code,
            "clazz": tool.clazz,
            "description": tool.description,
            "parameters": tool.parameters,
        }
    
    def _convert_agent_skill_to_dict(self, skill) -> Dict[str, Any]:
        """将 AgentSkill protobuf 对象转换为 Python 字典"""
        return {
            "id": skill.id,
            "skillName": skill.skillName,
            "skillDescription": skill.skillDescription,
            "skillContent": skill.skillContent,
            "skillType": skill.skillType,
        }
    
    def _convert_ai_agent_to_dict(self, agent) -> Dict[str, Any]:
        """将 AiAgent protobuf 对象转换为 Python 字典"""
        result = {
            "id": agent.id,
            "name": agent.name,
            "kbIds": agent.kbIds,
            "kbNames": agent.kbNames,
            "systemMessage": agent.systemMessage,
            "memoryCount": agent.memoryCount,
            "modelId": agent.modelId,
            "modelName": agent.modelName,
            "agentType": agent.agentType,
            "status": agent.status,
            "visitUrl": agent.visitUrl,
            "difyApiId": agent.difyApiId,
            "difyApiName": agent.difyApiName,
            "dayLmtPerClient": agent.dayLmtPerClient,
            "temperature": agent.temperature,
            "maxOutputToken": agent.maxOutputToken,
            "promptTemplate": agent.promptTemplate,
            "minScore": agent.minScore,
            "maxResult": agent.maxResult,
            "description": agent.description,
            "toolIds": agent.toolIds,
            "toolNames": agent.toolNames,
            "skillIds": agent.skillIds,
            "skillNames": agent.skillNames,
            "mcpToolIds": agent.mcpToolIds,
            "mcpToolNames": agent.mcpToolNames,
            "showReasoning": agent.showReasoning,
            "flowMemory": agent.flowMemory,
            "agentNodeId": agent.agentNodeId,
            "isRootAgent": agent.isRootAgent,
            "webSearch": agent.webSearch,
            "webSearchCount": agent.webSearchCount,
        }
        
        # 转换工具列表
        if agent.agentTools:
            result["agentTools"] = [self._convert_agent_tool_to_dict(tool) for tool in agent.agentTools]
        else:
            result["agentTools"] = []
        
        # 转换技能列表
        if agent.agentSkills:
            result["agentSkills"] = [self._convert_agent_skill_to_dict(skill) for skill in agent.agentSkills]
        else:
            result["agentSkills"] = []
        
        return result
    
    def get_ai_agent_by_id(self, agent_id: int, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
        """
        根据ID查询AIAgent（不含tool/skill详情）
        
        Args:
            agent_id: AI Agent ID
            timeout: 请求超时时间（秒），默认 10 秒
            
        Returns:
            AI Agent 信息字典，如果不存在则返回 None
            
        Raises:
            grpc.RpcError: gRPC 调用失败时抛出
            RuntimeError: 客户端未连接时抛出
        """
        if self.ai_agent_stub is None:
            raise RuntimeError("gRPC 客户端未连接，请先调用 connect() 方法")
        
        try:
            request = ai_agent_service_pb2.GetAiAgentByIdRequest(agentId=agent_id)
            response = self.ai_agent_stub.GetAiAgentById(request, timeout=timeout)
            
            if response.HasField("agent"):
                agent_dict = self._convert_ai_agent_to_dict(response.agent)
                logger.info(f"成功获取 AI Agent (ID: {agent_id}): {agent_dict.get('name', 'Unknown')}")
                return agent_dict
            else:
                logger.warning(f"未找到 ID 为 {agent_id} 的 AI Agent")
                return None
                
        except grpc.RpcError as e:
            logger.error(f"gRPC 调用失败: {e.code()} - {e.details()}")
            raise
        except Exception as e:
            logger.error(f"查询 AI Agent 时发生错误: {e}")
            raise
    
    def get_ai_agent_with_tool_and_skill_by_id(self, agent_id: int, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
        """
        根据ID查询AIAgent（含tool和skill详情）
        
        Args:
            agent_id: AI Agent ID
            timeout: 请求超时时间（秒），默认 10 秒
            
        Returns:
            AI Agent 信息字典（包含完整的工具和技能详情），如果不存在则返回 None
            
        Raises:
            grpc.RpcError: gRPC 调用失败时抛出
            RuntimeError: 客户端未连接时抛出
        """
        if self.ai_agent_stub is None:
            raise RuntimeError("gRPC 客户端未连接，请先调用 connect() 方法")
        
        try:
            request = ai_agent_service_pb2.GetAiAgentByIdRequest(agentId=agent_id)
            response = self.ai_agent_stub.GetAiAgentWithToolAndSkillById(request, timeout=timeout)
            
            if response.HasField("agent"):
                agent_dict = self._convert_ai_agent_to_dict(response.agent)
                logger.info(f"成功获取 AI Agent (ID: {agent_id}) 及其工具/技能详情: {agent_dict.get('name', 'Unknown')}")
                return agent_dict
            else:
                logger.warning(f"未找到 ID 为 {agent_id} 的 AI Agent")
                return None
                
        except grpc.RpcError as e:
            logger.error(f"gRPC 调用失败: {e.code()} - {e.details()}")
            raise
        except Exception as e:
            logger.error(f"查询 AI Agent 及详情时发生错误: {e}")
            raise


# ============ 向后兼容的别名 ============
# 保留 ModelGrpcClient 作为 GrpcClient 的别名，避免破坏已有引用
ModelGrpcClient = GrpcClient


# ============ 全局客户端实例（单例，所有服务共享） ============
_client_instance: Optional[GrpcClient] = None


def get_grpc_client() -> GrpcClient:
    """
    获取全局 gRPC 客户端实例（单例模式）
    
    Returns:
        GrpcClient 实例（同时支持 Model 和 AiAgent 服务）
    """
    global _client_instance
    if _client_instance is None:
        _client_instance = GrpcClient()
        _client_instance.connect()
    return _client_instance


def init_grpc_client(host: str = "127.0.0.1", port: int = 9090) -> GrpcClient:
    """
    初始化全局 gRPC 客户端（用于应用启动时调用）
    
    Args:
        host: gRPC 服务器地址
        port: gRPC 服务器端口
        
    Returns:
        GrpcClient 实例
    """
    global _client_instance
    if _client_instance is not None:
        _client_instance.close()
    
    _client_instance = GrpcClient(host=host, port=port)
    _client_instance.connect()
    logger.info(f"gRPC 客户端初始化完成: {host}:{port}")
    return _client_instance


def close_grpc_client() -> None:
    """
    关闭全局 gRPC 客户端（用于应用关闭时调用）
    """
    global _client_instance
    if _client_instance is not None:
        _client_instance.close()
        _client_instance = None
        logger.info("gRPC 客户端已关闭")


@contextmanager
def grpc_client_context(host: str = "127.0.0.1", port: int = 9090):
    """
    gRPC 客户端上下文管理器
    
    用法:
        with grpc_client_context() as client:
            models = client.list_models()
            agent = client.get_ai_agent_by_id(agent_id=1)
    
    Args:
        host: gRPC 服务器地址
        port: gRPC 服务器端口
        
    Yields:
        GrpcClient 实例
    """
    client = GrpcClient(host=host, port=port)
    try:
        client.connect()
        yield client
    finally:
        client.close()


if __name__ == "__main__":
    # 测试代码
    logging.basicConfig(level=logging.INFO)
    
    print("测试 gRPC 客户端...")
    print("请确保 Java gRPC 服务端已启动并监听 9090 端口")
    
    # 方式1: 使用上下文管理器
    print("\n=== 方式1: 使用上下文管理器 ===")
    try:
        with grpc_client_context(host="127.0.0.1", port=9090) as client:
            # 测试 Model 服务
            models = client.list_models()
            print(f"获取到 {len(models)} 个模型:")
            for model in models:
                print(f"  - {model['name']} (ID: {model['id']}, Provider: {model['provider']})")
            
            # 测试 AiAgent 服务（需要有效的 agent_id）
            # agent = client.get_ai_agent_by_id(agent_id=1)
            agent = client.get_ai_agent_with_tool_and_skill_by_id(agent_id=1)
            print(f"获取到agent: {agent}")
    except Exception as e:
        print(f"测试失败: {e}")
    
    # 方式2: 手动管理连接
    print("\n=== 方式2: 手动管理连接 ===")
    client = GrpcClient()
    try:
        client.connect()
        models = client.list_models()
        print(f"获取到 {len(models)} 个模型:")
        for model in models:
            print(f"  - {model['name']} (ID: {model['id']})")

        # agent = client.get_ai_agent_by_id(agent_id=1)
        agent = client.get_ai_agent_with_tool_and_skill_by_id(agent_id=1)
        print(f"获取到agent: {agent}")
    except Exception as e:
        print(f"测试失败: {e}")
    finally:
        client.close()
