-- 月经记录按登录用户隔离，同一天只允许一条记录。
CREATE TABLE IF NOT EXISTS menstrual_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  start_date DATE NOT NULL COMMENT '开始日期',
  end_date DATE DEFAULT NULL COMMENT '实际结束日期，为空时按七天估算',
  note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_menstrual_user_start (user_id, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人月经记录';
