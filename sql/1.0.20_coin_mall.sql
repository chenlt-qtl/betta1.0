-- 金币商城：商品、兑换快照和后台管理菜单。

CREATE TABLE IF NOT EXISTS `mall_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主键',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `images` text NOT NULL COMMENT '商品图片地址，英文逗号分隔，1至5张',
  `description` longtext NOT NULL COMMENT 'Markdown商品描述',
  `coin_price` bigint unsigned NOT NULL COMMENT '金币价格',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mall_product_name` (`name`)
) ENGINE=InnoDB COMMENT='商城商品';

CREATE TABLE IF NOT EXISTS `mall_exchange_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '兑换记录主键',
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `product_id` bigint NOT NULL COMMENT '原商品主键',
  `product_name` varchar(100) NOT NULL COMMENT '兑换时商品名称快照',
  `product_images` text NOT NULL COMMENT '兑换时商品图片快照',
  `coin_price` bigint unsigned NOT NULL COMMENT '兑换时金币价格快照',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mall_exchange_user_time` (`user_id`,`create_time`),
  KEY `idx_mall_exchange_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商城兑换记录';

-- 使用固定高位主键及 NOT EXISTS 保证菜单脚本可重复执行。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9200,'商城管理',0,9,'mall',NULL,'','',1,0,'M','0','0','','shopping','admin',NOW(),'商城后台管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9200);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9201,'商品管理',9200,1,'product','mall/product/index','','',1,0,'C','0','0','mall:product:list','shopping','admin',NOW(),'商城商品维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9201);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9202,'兑换记录',9200,2,'exchange','mall/exchange/index','','',1,0,'C','0','0','mall:exchange:list','list','admin',NOW(),'商城兑换记录查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9202);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9203,'商品查询',9201,1,'','','','',1,0,'F','0','0','mall:product:query','#','admin',NOW(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9203);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9204,'商品新增',9201,2,'','','','',1,0,'F','0','0','mall:product:add','#','admin',NOW(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9204);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9205,'商品修改',9201,3,'','','','',1,0,'F','0','0','mall:product:edit','#','admin',NOW(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9205);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9206,'商品删除',9201,4,'','','','',1,0,'F','0','0','mall:product:remove','#','admin',NOW(),''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9206);

-- 商城后台仅授权管理员角色，普通用户通过首页静态入口访问登录态商城。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,menu_id FROM sys_menu WHERE menu_id BETWEEN 9200 AND 9206;
