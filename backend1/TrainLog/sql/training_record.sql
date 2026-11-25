-- ==========================================================
-- TrainLog 训练记录表（backend1 / MySQL 5.7+）
-- 前提：先执行过 sql/sys_user.sql，库 trainlog 和 sys_user 表已存在
-- 说明：后端 datasource.url 带 createDatabaseIfNotExist=true，但只建库不建表，
--       所以这张表需要手动执行本脚本。
-- ==========================================================

USE trainlog;

CREATE TABLE IF NOT EXISTS training_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT       NOT NULL                COMMENT '所属用户，对应 sys_user.id',
    title            VARCHAR(80)  NOT NULL                COMMENT '训练项目，例如：卧推、5 公里慢跑',
    category         VARCHAR(16)  NOT NULL                COMMENT '训练大类：STRENGTH-力量训练 / CARDIO-有氧训练',
    type             VARCHAR(16)  NOT NULL                COMMENT '具体项目：CHEST/SHOULDER/BACK/LEG/INCLINE/CYCLING/RUNNING',
    training_date    DATE         NOT NULL                COMMENT '训练日期',
    duration_minutes INT          NOT NULL                COMMENT '时长（分钟）',
    distance_km      DECIMAL(6,2) DEFAULT NULL            COMMENT '有氧：距离（公里），力量训练为 NULL',
    sets             INT          DEFAULT NULL            COMMENT '力量：组数',
    reps             INT          DEFAULT NULL            COMMENT '力量：每组次数',
    weight_kg        DECIMAL(6,2) DEFAULT NULL            COMMENT '力量：重量（公斤），自重动作可为 NULL',
    calories         INT          DEFAULT NULL            COMMENT '消耗热量（千卡）',
    intensity        TINYINT      DEFAULT NULL            COMMENT '主观强度 RPE 1-10',
    notes            VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    created_at       DATETIME(6)  NOT NULL                COMMENT '创建时间',
    updated_at       DATETIME(6)  NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_training_user_date (user_id, training_date),
    KEY idx_training_user_type (user_id, type),
    KEY idx_training_user_category (user_id, category),
    CONSTRAINT fk_training_record_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '训练记录表';

-- ==========================================================
-- 编码约定（和前端 utils/trainingTypes.js 完全一致）
--
-- category = STRENGTH（力量训练）
--     type = CHEST     胸部训练
--     type = SHOULDER  肩部训练
--     type = BACK      背部训练
--     type = LEG       腿部训练
--     量化字段：sets 组数、reps 次数、weight_kg 重量；distance_km 为 NULL
--
-- category = CARDIO（有氧训练）
--     type = INCLINE   爬坡
--     type = CYCLING   骑车
--     type = RUNNING   跑步
--     量化字段：distance_km 距离；sets/reps/weight_kg 为 NULL
--
-- 约定：category 必须和 type 对应（CHEST 只能配 STRENGTH），
--       服务端写入前用枚举校验一次，避免出现"有氧 + 胸部训练"这种脏数据。
-- ==========================================================

-- ==========================================================
-- 可选的演示数据（admin 的 id 一般是 1，执行 sql/sys_user.sql 后才有该用户）
-- ==========================================================
-- INSERT INTO training_record
--     (user_id, title, category, type, training_date, duration_minutes, distance_km,
--      sets, reps, weight_kg, calories, intensity, notes, created_at, updated_at)
-- VALUES
--     (1, '卧推',      'STRENGTH', 'CHEST',    CURDATE(),                   50, NULL,  4, 8,  60.00, 300, 7, '最后一组力竭',        NOW(6), NOW(6)),
--     (1, '5 公里慢跑', 'CARDIO',   'RUNNING',  CURDATE() - INTERVAL 1 DAY,  32, 5.00, NULL, NULL, NULL, 310, 6, '配速 6''24"',     NOW(6), NOW(6)),
--     (1, '引体向上',   'STRENGTH', 'BACK',     CURDATE() - INTERVAL 2 DAY,  40, NULL,  4, 8,  NULL,  220, 8, '自重，最后一组 6 个',  NOW(6), NOW(6)),
--     (1, '动感单车',   'CARDIO',   'CYCLING',  CURDATE() - INTERVAL 3 DAY,  45, 18.50, NULL, NULL, NULL, 420, 7, '间歇骑行 8 组',      NOW(6), NOW(6)),
--     (1, '深蹲',      'STRENGTH', 'LEG',      CURDATE() - INTERVAL 4 DAY,  60, NULL,  5, 5, 100.00, 430, 8, '5x5，注意深度',      NOW(6), NOW(6)),
--     (1, '坡度走',     'CARDIO',   'INCLINE',  CURDATE() - INTERVAL 5 DAY,  40, 4.20, NULL, NULL, NULL, 280, 5, '坡度 12，速度 5.5',   NOW(6), NOW(6)),
--     (1, '推举',      'STRENGTH', 'SHOULDER', CURDATE() - INTERVAL 6 DAY,  45, NULL,  4, 10, 40.00, 260, 7, '坐姿哑铃推举',      NOW(6), NOW(6));
