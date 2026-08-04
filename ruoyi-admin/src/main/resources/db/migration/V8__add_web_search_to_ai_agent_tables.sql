ALTER TABLE ai_agent
    ADD COLUMN web_search tinyint not null default 0 COMMENT '是否开启联网搜索';

ALTER TABLE ai_agent
    ADD COLUMN web_search_count int not null default 3 COMMENT '联网搜索的结果数';

ALTER TABLE agent_kb_invoke
    ADD COLUMN url varchar(500) null comment '联网搜索的链接';

ALTER TABLE agent_kb_invoke
    MODIFY COLUMN file_name varchar(255) null comment '文件名';
