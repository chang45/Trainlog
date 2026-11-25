-- TrainLog MySQL 建库建表脚本（后端 ddl-auto=update 也会自动建表，此脚本用于手动初始化）
CREATE DATABASE IF NOT EXISTS trainlog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE trainlog;

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname      VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    enabled       BIT(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    created_at    DATETIME(6)  NOT NULL COMMENT '创建时间',
    last_login_at DATETIME(6)  DEFAULT NULL COMMENT '最后登录时间',
    login_count   INT          NOT NULL DEFAULT 0 COMMENT '累计登录次数',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS training_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL COMMENT '所属用户',
    title            VARCHAR(80)  NOT NULL COMMENT '训练项目',
    type             VARCHAR(20)  NOT NULL COMMENT '训练类型枚举',
    training_date    DATE         NOT NULL COMMENT '训练日期',
    duration_minutes INT          NOT NULL COMMENT '时长（分钟）',
    distance_km      DOUBLE       DEFAULT NULL COMMENT '距离（公里）',
    calories         INT          DEFAULT NULL COMMENT '消耗热量（千卡）',
    intensity        INT          DEFAULT NULL COMMENT '主观强度 RPE 1-10',
    notes            VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME(6)  NOT NULL COMMENT '创建时间',
    updated_at       DATETIME(6)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_training_user_date (user_id, training_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '训练记录表';
