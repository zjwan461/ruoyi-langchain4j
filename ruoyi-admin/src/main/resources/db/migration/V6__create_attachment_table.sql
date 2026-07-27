drop table if exists attachment;
create table attachment
(
    id              bigint unsigned not null auto_increment comment '主键ID',
    file_name       varchar(255) not null comment '文件名',
    file_ext        varchar(10)  not null comment '文件后缀',
    file_content    longtext     not null comment '文件内容',
    message_id      bigint unsigned null comment 'message_id',
    message_flow_id bigint unsigned null comment 'message_flow_id',
    create_time     DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    primary key (id)
) engine = innodb
    comment ='聊天附件表';

