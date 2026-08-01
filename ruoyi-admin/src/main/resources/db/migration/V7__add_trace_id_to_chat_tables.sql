-- 为 chat_message 表添加 trace_id 字段
ALTER TABLE chat_message ADD COLUMN trace_id varchar(100) COMMENT '追踪ID';

-- 为 chat_flow_message 表添加 trace_id 字段
ALTER TABLE chat_flow_message ADD COLUMN trace_id varchar(100) COMMENT '追踪ID';
