CREATE TABLE IF NOT EXISTS user_nutrition_target (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  daily_calories INT DEFAULT NULL COMMENT 'daily calories',
  protein_normal INT DEFAULT NULL COMMENT 'normal protein grams',
  protein_bulk INT DEFAULT NULL COMMENT 'bulk protein grams',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id),
  CONSTRAINT fk_nutrition_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user nutrition target';

INSERT INTO user_nutrition_target (user_id, daily_calories, protein_normal, protein_bulk, created_at, updated_at)
SELECT id, daily_calories, protein_normal, protein_bulk, NOW(6), NOW(6) FROM sys_user
ON DUPLICATE KEY UPDATE daily_calories = VALUES(daily_calories), protein_normal = VALUES(protein_normal), protein_bulk = VALUES(protein_bulk), updated_at = VALUES(updated_at);

ALTER TABLE sys_user DROP COLUMN daily_calories, DROP COLUMN protein_normal, DROP COLUMN protein_bulk;

SHOW COLUMNS FROM sys_user;
SELECT * FROM user_nutrition_target;