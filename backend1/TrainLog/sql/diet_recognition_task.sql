-- ==========================================================
-- TrainLog 饮食图片异步识别任务（MySQL 5.7+）
-- ==========================================================

USE trainlog;

CREATE TABLE IF NOT EXISTS diet_recognition_task (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id       VARCHAR(64) NOT NULL                COMMENT '任务ID（UUID）',
    user_id       BIGINT      NOT NULL                COMMENT '用户ID',
    meal_type     VARCHAR(16) NOT NULL                COMMENT '餐次',
    status        VARCHAR(16) NOT NULL                COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
    record_id     BIGINT      DEFAULT NULL            COMMENT '识别成功后写入的 diet_record.id',
    error_message VARCHAR(500) DEFAULT NULL           COMMENT '失败原因',
    created_at    DATETIME(6) NOT NULL                COMMENT '创建时间',
    updated_at    DATETIME(6) NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_task_user (user_id, created_at),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '饮食图片异步识别任务';
