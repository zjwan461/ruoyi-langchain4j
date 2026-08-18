ALTER TABLE chat_flow_message
    MODIFY COLUMN agent_id bigint unsigned null default null comment '智能体ID';

ALTER TABLE chat_flow_message
    MODIFY COLUMN agent_name varchar(255) null default null comment '智能体名称';