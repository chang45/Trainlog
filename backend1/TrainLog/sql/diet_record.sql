-- ==========================================================
-- TrainLog 每日饮食记录（MySQL 5.7+）
-- user_id 外键指向 sys_user.id
-- ==========================================================

USE trainlog;

CREATE TABLE IF NOT EXISTS diet_record (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL                COMMENT '用户ID，外键 sys_user.id',
    meal_type  VARCHAR(16)  NOT NULL                COMMENT '餐次：BREAKFAST/LUNCH/DINNER',
    food_name  VARCHAR(100) NOT NULL                COMMENT '食物名',
    calories   INT          DEFAULT NULL            COMMENT '热量（千卡）',
    protein    DECIMAL(6,1) DEFAULT NULL            COMMENT '蛋白质（克）',
    carbs      DECIMAL(6,1) DEFAULT NULL            COMMENT '碳水（克）',
    fat        DECIMAL(6,1) DEFAULT NULL            COMMENT '脂肪（克）',
    eaten_date DATE         NOT NULL                COMMENT '日期',
    created_at DATETIME(6)  NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_diet_user_date (user_id, eaten_date),
    CONSTRAINT fk_diet_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '每日饮食记录';
