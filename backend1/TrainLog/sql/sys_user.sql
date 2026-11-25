-- ==========================================================
-- TrainLog 用户表建表脚本（MySQL 5.7+）
-- 对应配置：application-dev.yml -> trainlog.datasource.database = trainlog
-- 执行方式：Navicat / IDEA Database / mysql 命令行 均可
-- ==========================================================

CREATE DATABASE IF NOT EXISTS trainlog
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE trainlog;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username      VARCHAR(50)  NOT NULL                COMMENT '登录名（唯一）',
    password_hash VARCHAR(255) NOT NULL                COMMENT '密码（Argon2id 密文）',
    nickname      VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用：1-启用，0-禁用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_at DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
    login_count   INT          NOT NULL DEFAULT 0      COMMENT '累计登录次数',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表';

-- 可选：插入测试账号（密码密文需用程序生成，见下方说明）
-- INSERT INTO sys_user (username, password_hash, nickname, enabled, created_at)
-- VALUES ('admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '管理员', 1, NOW());
