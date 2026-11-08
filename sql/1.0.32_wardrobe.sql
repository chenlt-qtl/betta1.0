-- 个人衣橱：图片置于私有表，通过鉴权接口读取。
CREATE TABLE IF NOT EXISTS wardrobe_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '单品主键',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '单品名称',
  category VARCHAR(20) NOT NULL COMMENT '单品分类',
  color VARCHAR(40) NOT NULL DEFAULT '' COMMENT '单品颜色',
  seasons_json JSON NOT NULL COMMENT '适用季节JSON数组',
  occasions_json JSON NOT NULL COMMENT '适用场合JSON数组',
  note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id), KEY idx_wardrobe_item_user_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人衣橱单品';

CREATE TABLE IF NOT EXISTS wardrobe_item_image (
  item_id BIGINT NOT NULL COMMENT '单品ID',
  content_type VARCHAR(30) NOT NULL COMMENT '图片媒体类型',
  image_content MEDIUMBLOB NOT NULL COMMENT '图片字节',
  PRIMARY KEY (item_id),
  CONSTRAINT fk_wardrobe_item_image_item FOREIGN KEY (item_id) REFERENCES wardrobe_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣橱单品私有图片';

CREATE TABLE IF NOT EXISTS wardrobe_outfit (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '搭配主键',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  name VARCHAR(100) NOT NULL COMMENT '搭配名称',
  occasions_json JSON NOT NULL COMMENT '适用场合JSON数组',
  favorite TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否收藏：0否，1是',
  sort_order BIGINT NOT NULL DEFAULT 0 COMMENT '个人搭配排序值',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id), KEY idx_wardrobe_outfit_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人搭配';

CREATE TABLE IF NOT EXISTS wardrobe_outfit_image (
  outfit_id BIGINT NOT NULL COMMENT '搭配ID',
  content_type VARCHAR(30) NOT NULL COMMENT '效果照媒体类型',
  image_content MEDIUMBLOB NOT NULL COMMENT '效果照图片字节',
  PRIMARY KEY (outfit_id),
  CONSTRAINT fk_wardrobe_outfit_image_outfit FOREIGN KEY (outfit_id) REFERENCES wardrobe_outfit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配私有效果照';

CREATE TABLE IF NOT EXISTS wardrobe_outfit_item (
  outfit_id BIGINT NOT NULL COMMENT '搭配ID',
  item_id BIGINT NOT NULL COMMENT '单品ID',
  sort_order INT NOT NULL COMMENT '单品展示顺序，从0开始',
  PRIMARY KEY (outfit_id, item_id), KEY idx_wardrobe_outfit_item_item (item_id),
  CONSTRAINT fk_wardrobe_relation_outfit FOREIGN KEY (outfit_id) REFERENCES wardrobe_outfit(id) ON DELETE CASCADE,
  CONSTRAINT fk_wardrobe_relation_item FOREIGN KEY (item_id) REFERENCES wardrobe_item(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配单品顺序';

