ALTER TABLE chat_flow_message
    MODIFY COLUMN agent_id bigint unsigned null default null comment '智能体ID';

ALTER TABLE chat_flow_message
    MODIFY COLUMN agent_name varchar(255) null default null comment '智能体名称';


ALTER TABLE agent_kb_invoke
    ADD column message_flow_id bigint unsigned null default null comment 'chat flow message ID';

ALTER TABLE agent_kb_invoke
    MODIFY column message_id bigint unsigned null default null comment 'chat message ID';

ALTER TABLE agent_tool_invoke
    ADD column message_flow_id bigint unsigned null default null comment 'chat flow message ID';

ALTER TABLE agent_tool_invoke
    MODIFY column message_id bigint unsigned null default null comment 'chat message ID';