drop table if exists attachment;
create table attachment
(
    id              bigint unsigned not null auto_increment comment '主键ID',
    file_name       varchar(255)    not null comment '文件名',
    file_ext        varchar(10)     not null comment '文件后缀',
    file_content    longtext        not null comment '文件内容',
    file_url        varchar(255)    not null comment '文件链接',
    message_id      bigint unsigned null comment 'message_id',
    message_flow_id bigint unsigned null comment 'message_flow_id',
    create_time     DATETIME        NULL DEFAULT NULL COMMENT '创建时间',
    primary key (id)
) engine = innodb
    comment ='聊天附件表';

INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
                     create_by, create_time, update_by, update_time, remark)
VALUES (100, '聊天文件清理', 'DEFAULT', 'houseKeepTask.cleanAttachments', '0 0 0 * * ?', '1', '1', '0', 'admin',
        '2026-07-30 13:52:58', '', '2026-07-30 13:53:04', '');
