drop table if exists agent_tool;
create table agent_tool
(
    id          bigint unsigned not null comment '主键ID',
    tool_name   varchar(255)    not null comment '工具名称',
    tool_type   varchar(255)    not null comment '工具类型，可选项：builtin,sandbox',
    code        longtext        null comment '工具代码，仅sandbox类型有值',
    clazz       varchar(255)    null comment '内置工具全类名，仅builtin类型有值',
    create_time DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time datetime        null default null comment '更新时间',
    create_by   varchar(64)          default '' comment '创建者',
    update_by   varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_tool表';
