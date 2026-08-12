#!/usr/bin/env bash
# ============================================================
#  生成 gRPC Python 源码 (Linux / macOS / Git Bash)
#  使用方法：在 agentic/ 目录下执行  ./gen_grpc.sh
# ============================================================
set -e

# 切换到脚本所在目录
cd "$(dirname "$0")"

echo "========================================"
echo "  生成 gRPC Python 源码"
echo "========================================"
echo

# 1. 检查 Python 是否可用
if ! command -v python >/dev/null 2>&1; then
    echo "[错误] 未检测到 python，请先安装 Python 并加入 PATH。"
    exit 1
fi

# 2. 检查 grpcio-tools 是否已安装
if ! python -c "import grpc_tools" 2>/dev/null; then
    echo "[警告] 未检测到 grpcio-tools，正在尝试安装依赖..."
    echo
    python -m pip install -r requirements.txt
fi

# 3. 确保输出目录存在
mkdir -p grpc_generated

# 4. 执行 protoc 生成命令
echo "正在生成代码..."
python -m grpc_tools.protoc \
    -I proto \
    --python_out=grpc_generated \
    --grpc_python_out=grpc_generated \
    proto/model_service.proto

echo
echo "[成功] 已生成 grpc_generated/model_service_pb2.py"
echo "[成功] 已生成 grpc_generated/model_service_pb2_grpc.py"