-- 商城人民币定价、动态汇率及汇率设置权限。

-- 兼容重复执行：仅在人民币价格列不存在时新增。
SET @yuan_price_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mall_product' AND COLUMN_NAME = 'yuan_price'
);
SET @yuan_price_sql = IF(
  @yuan_price_exists = 0,
  'ALTER TABLE mall_product ADD COLUMN yuan_price DECIMAL(21,2) NULL COMMENT ''人民币价格'' AFTER description',
  'SELECT 1'
);
PREPARE stmt FROM @yuan_price_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 旧商品按原金币价格回填，默认汇率为一时保持原价格不变。
UPDATE mall_product SET yuan_price = coin_price WHERE yuan_price IS NULL;
ALTER TABLE mall_product MODIFY COLUMN yuan_price DECIMAL(21,2) NOT NULL COMMENT '人民币价格';

INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT '商城-人民币兑金币汇率','mall.rmbCoinRate','1','Y','admin',NOW(),'一元人民币可兑换的金币数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='mall.rmbCoinRate');

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9207,'汇率设置',9201,5,'','','','',1,0,'F','0','0','mall:product:rate','#','admin',NOW(),'商城人民币兑金币汇率设置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9207);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES(1,9207);
