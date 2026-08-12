"""
gRPC 客户端封装模块
用于调用 Java 后端的 ModelGrpcService 服务
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

logger = logging.getLogger(__name__)


class ModelGrpcClient:
    """模型服务 gRPC 客户端"""
    
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
        self.stub: Optional[model_service_pb2_grpc.ModelGrpcServiceStub] = None
        
    def connect(self) -> None:
        """建立 gRPC 连接"""
        if self.channel is None:
            target = f"{self.host}:{self.port}"
            self.channel = grpc.insecure_channel(target)
            self.stub = model_service_pb2_grpc.ModelGrpcServiceStub(self.channel)
            logger.info(f"已连接到 gRPC 服务器: {target}")
    
    def close(self) -> None:
        """关闭 gRPC 连接"""
        if self.channel is not None:
            self.channel.close()
            self.channel = None
            self.stub = None
            logger.info("已关闭 gRPC 连接")
    
    def __enter__(self):
        """支持 with 语句"""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """退出 with 语句时自动关闭连接"""
        self.close()
    
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
        if self.stub is None:
            raise RuntimeError("gRPC 客户端未连接，请先调用 connect() 方法")
        
        try:
            # 创建空请求
            request = model_service_pb2.ListModelRequest()
            
            # 调用 gRPC 方法
            response = self.stub.ListModel(request, timeout=timeout)
            
            # 转换响应为 Python 字典列表
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


# 全局客户端实例（用于 FastAPI 依赖注入）
_client_instance: Optional[ModelGrpcClient] = None


def get_grpc_client() -> ModelGrpcClient:
    """
    获取全局 gRPC 客户端实例（单例模式）
    
    Returns:
        ModelGrpcClient 实例
    """
    global _client_instance
    if _client_instance is None:
        _client_instance = ModelGrpcClient()
        _client_instance.connect()
    return _client_instance


def init_grpc_client(host: str = "127.0.0.1", port: int = 9090) -> ModelGrpcClient:
    """
    初始化全局 gRPC 客户端（用于应用启动时调用）
    
    Args:
        host: gRPC 服务器地址
        port: gRPC 服务器端口
        
    Returns:
        ModelGrpcClient 实例
    """
    global _client_instance
    if _client_instance is not None:
        _client_instance.close()
    
    _client_instance = ModelGrpcClient(host=host, port=port)
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
    
    Args:
        host: gRPC 服务器地址
        port: gRPC 服务器端口
        
    Yields:
        ModelGrpcClient 实例
    """
    client = ModelGrpcClient(host=host, port=port)
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
            models = client.list_models()
            print(f"获取到 {len(models)} 个模型:")
            for model in models:
                print(f"  - {model['name']} (ID: {model['id']}, Provider: {model['provider']})")
    except Exception as e:
        print(f"测试失败: {e}")
    
    # 方式2: 手动管理连接
    print("\n=== 方式2: 手动管理连接 ===")
    client = ModelGrpcClient()
    try:
        client.connect()
        models = client.list_models()
        print(f"获取到 {len(models)} 个模型:")
        for model in models:
            print(f"  - {model['name']} (ID: {model['id']})")
    except Exception as e:
        print(f"测试失败: {e}")
    finally:
        client.close()
