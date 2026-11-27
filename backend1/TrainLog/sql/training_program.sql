-- ==========================================================
-- TrainLog 训练计划表（backend1 / MySQL 5.7+）
-- 前提：先执行过 sql/sys_user.sql
-- 一个用户可以有多套计划，active=1 表示当前计划（每人最多一个）
-- 完成状态不落库，由前端拿该用户当周 training_record 实时比对
-- ==========================================================

USE trainlog;

CREATE TABLE IF NOT EXISTS training_program (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL                COMMENT '所属用户',
    name        VARCHAR(50)  NOT NULL                COMMENT '计划名，例如：一周四练-增肌',
    description VARCHAR(200) DEFAULT NULL            COMMENT '描述',
    goal        VARCHAR(16)  NOT NULL DEFAULT 'MAINTAIN' COMMENT '计划目标：BULK 增肌 / CUT 减脂 / MAINTAIN 维持',
    active      BOOLEAN      NOT NULL DEFAULT FALSE  COMMENT '是否当前计划，每人最多一个',
    created_at  DATETIME(6)  NOT NULL                COMMENT '创建时间',
    updated_at  DATETIME(6)  NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_program_user (user_id),
    CONSTRAINT fk_program_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '训练计划';

CREATE TABLE IF NOT EXISTS training_program_item (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    program_id BIGINT      NOT NULL                COMMENT '所属计划',
    category   VARCHAR(16) NOT NULL                COMMENT '训练大类：STRENGTH / CARDIO',
    type       VARCHAR(16) DEFAULT NULL            COMMENT '具体项目：CHEST/.../RUNNING，NULL 表示任意',
    goal       VARCHAR(16) DEFAULT NULL            COMMENT '阶段目标：BULK 增肌 / CUT 减脂',
    note       VARCHAR(60) DEFAULT NULL            COMMENT '备注',
    sort       INT         NOT NULL DEFAULT 0      COMMENT '同一天内的排序',
    PRIMARY KEY (id),
    KEY idx_item_program (program_id),
    CONSTRAINT fk_item_program FOREIGN KEY (program_id)
        REFERENCES training_program (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '训练计划项';
