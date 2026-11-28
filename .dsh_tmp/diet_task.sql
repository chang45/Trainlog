CREATE TABLE IF NOT EXISTS diet_recognition_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  meal_type VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  record_id BIGINT DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_id (task_id),
  KEY idx_task_user (user_id, created_at),
  CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='diet recognition task';
SHOW COLUMNS FROM diet_recognition_task;