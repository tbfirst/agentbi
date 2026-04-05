-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `user_role` VARCHAR(20) DEFAULT 'user' COMMENT '用户角色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete` TINYINT DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 智能 bi 图表信息表
create table if not exists chart
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint                             null comment '用户id',
    fingerprint varchar(64)                        null comment '文件指纹(用于缓存命中判断)',
    name        varchar(128)                       null comment '图表名称',
    goal        text                               null comment '分析目标',
    chartData   text                               null comment '图表数据',
    chartType   varchar(128)                       null comment '图表类型',
    aiChart     text                               null comment 'ai 生成的图表数据',
    aiResult    text                               null comment 'ai 生成的分析结论',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_fingerprint (fingerprint),
    INDEX idx_user_id (userId),
    INDEX idx_create_time (createTime)
) comment '智能 bi 图表信息表' collate = utf8mb4_unicode_ci;

-- 文件解析任务表
CREATE TABLE file_task_info (
     id              BIGINT PRIMARY KEY AUTO_INCREMENT,
     file_task_id    VARCHAR(64) UNIQUE NOT NULL COMMENT '文件任务ID',
     user_id         VARCHAR(64) NOT NULL COMMENT '用户ID',
     fingerprint     VARCHAR(64) NOT NULL COMMENT '文件指纹',
     original_url    VARCHAR(512) NOT NULL COMMENT '原始文件OSS地址',
     csv_url         VARCHAR(512) COMMENT '解析后CSV地址',
     status          VARCHAR(64) NOT NULL DEFAULT 0 COMMENT 'PENDING(待处理)、RUNNING(执行中)、SUCCEEDED(成功)、FAILED(失败)',
     error_msg       TEXT COMMENT '错误信息',
     file_size       BIGINT COMMENT '原始文件大小',
     csv_size        BIGINT COMMENT 'CSV文件大小',
     parse_time_ms   INT COMMENT '解析耗时',
     created_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
     updated_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

     INDEX idx_user_id (user_id),
     INDEX idx_fingerprint (fingerprint),
     INDEX idx_status_created (status, created_time)
) ENGINE=InnoDB COMMENT='文件解析任务表';
