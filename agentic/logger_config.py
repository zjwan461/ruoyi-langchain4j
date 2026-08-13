# logger_config.py
import sys
import logging
import os
from logging.handlers import TimedRotatingFileHandler
from contextvars import ContextVar

# 自动创建 logs 目录
LOG_DIR = "logs"
if not os.path.exists(LOG_DIR):
    os.makedirs(LOG_DIR)

# 全局 trace_id 上下文
trace_id_ctx: ContextVar[str] = ContextVar("trace_id", default="")


# 自定义日志格式（自动带上 trace_id）
class TraceFormatter(logging.Formatter):
    def format(self, record):
        record.trace_id = trace_id_ctx.get()
        return super().format(record)


# 日志配置：控制台 + 文件（按天分割）
def get_logger():
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)
    logger.propagate = False

    # 避免重复添加 handler
    if not logger.handlers:
        formatter = TraceFormatter(
            "%(asctime)s | %(trace_id)s | %(levelname)s | %(message)s"
        )

        # 控制台输出
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)

        # 文件输出
        file_handler = TimedRotatingFileHandler(
            os.path.join(LOG_DIR, "agentic.log"),
            when="midnight",
            backupCount=7,
            encoding="utf-8",
        )
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)

    return logger


# 导出全局 logger 和 trace_id 上下文
logger = get_logger()
__all__ = ["logger", "trace_id_ctx"]
