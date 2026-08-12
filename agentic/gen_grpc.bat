@echo off
REM ============================================================
REM  生成 gRPC Python 源码 (Windows)
REM  使用方法：在 agentic/ 目录下双击或 cmd 执行本脚本
REM ============================================================
setlocal

REM 切换到脚本所在目录
cd /d "%~dp0"

echo ========================================
echo   生成 gRPC Python 源码
echo ========================================
echo.

REM 1. 检查 Python 是否可用
where python >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 python，请先安装 Python 并加入 PATH。
    echo.
    pause
    exit /b 1
)

REM 2. 检查 grpcio-tools 是否已安装
python -c "import grpc_tools" 2>nul
if errorlevel 1 (
    echo [警告] 未检测到 grpcio-tools，正在尝试安装依赖...
    echo.
    python -m pip install -r requirements.txt
    if errorlevel 1 (
        echo [错误] 依赖安装失败，请手动执行：pip install -r requirements.txt
        echo.
        pause
        exit /b 1
    )
)

REM 3. 确保输出目录存在
if not exist grpc_generated mkdir grpc_generated

REM 4. 执行 protoc 生成命令
echo 正在生成代码...
python -m grpc_tools.protoc ^
    -I proto ^
    --python_out=grpc_generated ^
    --grpc_python_out=grpc_generated ^
    proto\model_service.proto

if errorlevel 1 (
    echo.
    echo [错误] 生成失败，请检查上方日志。
    pause
    exit /b 1
)

echo.
echo [成功] 已生成 grpc_generated\model_service_pb2.py
echo [成功] 已生成 grpc_generated\model_service_pb2_grpc.py
echo.
pause
endlocal