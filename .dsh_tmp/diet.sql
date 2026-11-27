CREATE TABLE IF NOT EXISTS diet_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  meal_type VARCHAR(16) NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER',
  food_name VARCHAR(100) NOT NULL,
  calories INT DEFAULT NULL,
  protein DECIMAL(6,1) DEFAULT NULL,
  carbs DECIMAL(6,1) DEFAULT NULL,
  fat DECIMAL(6,1) DEFAULT NULL,
  eaten_date DATE NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_diet_user_date (user_id, eaten_date),
  CONSTRAINT fk_diet_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='diet record';
SHOW COLUMNS FROM diet_record;