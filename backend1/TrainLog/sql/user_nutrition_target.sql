-- ==========================================================
-- TrainLog 用户营养目标表（MySQL 5.7+）
-- user_id 外键指向 sys_user.id，一个用户一行
-- ==========================================================

USE trainlog;

CREATE TABLE IF NOT EXISTS user_nutrition_target (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id        BIGINT      NOT NULL                COMMENT '用户ID，外键 sys_user.id',
    daily_calories INT         DEFAULT NULL            COMMENT '每日总消耗（千卡）',
    protein_normal INT         DEFAULT NULL            COMMENT '正常维持蛋白质（克/天）',
    protein_bulk   INT         DEFAULT NULL            COMMENT '增肌蛋白质（克/天）',
    protein_cut    INT         DEFAULT NULL            COMMENT '减脂蛋白质（克/天）',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrition_user (user_id),
    CONSTRAINT fk_nutrition_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户营养目标';
