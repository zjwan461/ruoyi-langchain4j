drop table if exists agent_skill;
create table agent_skill
(
    id                bigint unsigned not null auto_increment comment '主键ID',
    skill_name        varchar(255)    not null comment 'skill名称',
    skill_description varchar(500)    not null comment 'skill描述',
    skill_content     longtext        not null comment 'skill内容',
    create_time       DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    update_time       datetime        null default null comment '更新时间',
    create_by         varchar(64)          default '' comment '创建者',
    update_by         varchar(64)          default '' comment '更新者',
    primary key (id)
) engine = innodb
    comment ='agent_skill表';