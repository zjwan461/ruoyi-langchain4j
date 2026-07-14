drop table if exists agent_mcp_service;
create table agent_mcp_service
(
    id               bigint unsigned not null auto_increment comment '主键ID',
    mcp_service_name varchar(255)    not null comment 'mcp服务名称',
    mcp_url          varchar(500)    not null comment 'mcp链接',
    mcp_transport    varchar(255)    not null comment 'mcp transport',
    create_time      DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time      datetime        null default null comment '更新时间',
    create_by        varchar(64)          default '' comment '创建者',
    update_by        varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_mcp_service表';

drop table if exists agent_mcp_tool;
create table agent_mcp_tool
(
    id                   bigint unsigned not null auto_increment comment '主键ID',
    mcp_tool_name        varchar(255)    not null comment 'mcp工具名称',
    mcp_tool_description varchar(500)    not null comment 'mcp工具描述',
    mcp_tool_parameters  longtext        not null comment 'mcp工具参数',
    pid                  bigint unsigned not null comment '所属mcp service id',
    create_time          DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time          datetime        null default null comment '更新时间',
    create_by            varchar(64)          default '' comment '创建者',
    update_by            varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_mcp_tool表';