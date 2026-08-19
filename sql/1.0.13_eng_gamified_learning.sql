-- 完整游戏化英语学习模块：仅新增三张游戏化表，不修改既有八张英语基础表。

CREATE TABLE IF NOT EXISTS `eng_study_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '学习记录主键',
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `article_id` bigint NOT NULL COMMENT '文章主键',
  `score` int NOT NULL DEFAULT 0 COMMENT '本次百分制得分',
  `correct_count` int NOT NULL DEFAULT 0 COMMENT '正确题数',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总题数',
  `passed` tinyint NOT NULL DEFAULT 0 COMMENT '是否通关：0否，1是',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eng_study_record_user_time` (`user_id`,`create_time`),
  KEY `idx_eng_study_record_article` (`article_id`)
) ENGINE=InnoDB COMMENT='英语闯关学习记录';

CREATE TABLE IF NOT EXISTS `eng_article_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章进度主键',
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `article_id` bigint NOT NULL COMMENT '文章主键',
  `best_score` int NOT NULL DEFAULT 0 COMMENT '历史最好百分制得分',
  `best_correct_count` int NOT NULL DEFAULT 0 COMMENT '最好成绩正确题数',
  `best_total_count` int NOT NULL DEFAULT 0 COMMENT '最好成绩总题数',
  `completed` tinyint NOT NULL DEFAULT 0 COMMENT '是否通关：0否，1是',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_article_progress_user_article` (`user_id`,`article_id`),
  KEY `idx_eng_article_progress_completed` (`user_id`,`completed`)
) ENGINE=InnoDB COMMENT='英语文章最好进度';

CREATE TABLE IF NOT EXISTS `eng_wrong_word` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '错词记录主键',
  `user_id` bigint NOT NULL COMMENT '用户主键',
  `article_id` bigint NOT NULL COMMENT '来源文章主键',
  `word_id` bigint NOT NULL COMMENT '单词主键',
  `wrong_count` int NOT NULL DEFAULT 1 COMMENT '累计错误次数',
  `mastered` tinyint NOT NULL DEFAULT 0 COMMENT '掌握状态：0未掌握，1已掌握',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_wrong_word_user_article_word` (`user_id`,`article_id`,`word_id`),
  KEY `idx_eng_wrong_word_user_mastered` (`user_id`,`mastered`)
) ENGINE=InnoDB COMMENT='英语用户错词本';

-- 使用固定高位主键及 NOT EXISTS 保证菜单脚本可重复执行。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9100,'英语学习',0,8,'eng',NULL,'','',1,0,'M','0','0','','education','admin',NOW(),'英语学习菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9100);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9101,'英语管理',9100,1,'manage','ParentView','','',1,0,'M','0','0','','documentation','admin',NOW(),'英语维护菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9101);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9102,'文章管理',9101,1,'article','eng/article/index','','',1,0,'C','0','0','eng:article:list','documentation','admin',NOW(),'英语文章维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9102);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9103,'分组管理',9101,2,'group','eng/group/index','','',1,0,'C','0','0','eng:group:list','tree','admin',NOW(),'英语分组维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9103);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9104,'句子管理',9101,3,'sentence','eng/sentence/index','','',1,0,'C','0','0','eng:sentence:list','list','admin',NOW(),'英语句子维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9104);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9105,'单词管理',9101,4,'word','eng/word/index','','',1,0,'C','0','0','eng:word:list','language','admin',NOW(),'英语单词维护'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9105);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9113,'查单词',9101,5,'word-query','eng/word/newWord','','',1,0,'C','0','0','eng:score:list','search','admin',NOW(),'英语单词查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9113);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9106,'播放列表',9100,2,'playlist','eng/playlist/index','','',1,0,'C','0','0','eng:playList:list','play','admin',NOW(),'英语播放列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9106);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9107,'文章详情',9100,10,'article-detail/:articleId','eng/article/detail/index','','',1,0,'C','1','0','eng:article:query','#','admin',NOW(),'文章详情隐藏路由'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9107);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9108,'文章跟读',9100,11,'article/read/:articleId','eng/article/read/index','','',1,0,'C','1','0','eng:sentence:list','#','admin',NOW(),'文章跟读隐藏路由'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9108);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9109,'文章测试',9100,12,'article/test/:articleId','eng/article/test/index','','',1,0,'C','1','0','eng:score:list','#','admin',NOW(),'文章测试隐藏路由'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9109);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9110,'学习中心',9100,3,'study/index','eng/study/index','','',1,0,'C','0','0','eng:study:view','guide','admin',NOW(),'游戏化学习中心'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9110);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9111,'文章闯关',9100,13,'study/challenge/:articleId','eng/study/challenge','','',1,0,'C','1','0','eng:study:challenge','star','admin',NOW(),'文章闯关隐藏路由'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9111);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9112,'错词本',9100,4,'study/wrong','eng/study/wrong','','',1,0,'C','0','0','eng:study:wrong','skill','admin',NOW(),'用户错词本'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9112);

-- 为基础维护接口逐项创建实际按钮权限，不使用项目不支持的通配权限。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9120,'文章查询',9102,1,'','','','',1,0,'F','0','0','eng:article:query','#','admin',NOW(),'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9120);
INSERT INTO sys_menu SELECT 9121,'文章新增',9102,2,'','','','',1,0,'F','0','0','eng:article:add','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9121);
INSERT INTO sys_menu SELECT 9122,'文章修改',9102,3,'','','','',1,0,'F','0','0','eng:article:edit','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9122);
INSERT INTO sys_menu SELECT 9123,'文章删除',9102,4,'','','','',1,0,'F','0','0','eng:article:remove','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9123);
INSERT INTO sys_menu SELECT 9124,'文章导出',9102,5,'','','','',1,0,'F','0','0','eng:article:export','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9124);
INSERT INTO sys_menu SELECT 9130,'分组查询',9103,1,'','','','',1,0,'F','0','0','eng:group:query','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9130);
INSERT INTO sys_menu SELECT 9131,'分组新增',9103,2,'','','','',1,0,'F','0','0','eng:group:add','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9131);
INSERT INTO sys_menu SELECT 9132,'分组修改',9103,3,'','','','',1,0,'F','0','0','eng:group:edit','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9132);
INSERT INTO sys_menu SELECT 9133,'分组删除',9103,4,'','','','',1,0,'F','0','0','eng:group:remove','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9133);
INSERT INTO sys_menu SELECT 9134,'分组导出',9103,5,'','','','',1,0,'F','0','0','eng:group:export','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9134);
INSERT INTO sys_menu SELECT 9140,'句子查询',9104,1,'','','','',1,0,'F','0','0','eng:sentence:query','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9140);
INSERT INTO sys_menu SELECT 9141,'句子新增',9104,2,'','','','',1,0,'F','0','0','eng:sentence:add','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9141);
INSERT INTO sys_menu SELECT 9142,'句子修改',9104,3,'','','','',1,0,'F','0','0','eng:sentence:edit','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9142);
INSERT INTO sys_menu SELECT 9143,'句子删除',9104,4,'','','','',1,0,'F','0','0','eng:sentence:remove','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9143);
INSERT INTO sys_menu SELECT 9144,'句子导出',9104,5,'','','','',1,0,'F','0','0','eng:sentence:export','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9144);
INSERT INTO sys_menu SELECT 9150,'单词查询',9105,1,'','','','',1,0,'F','0','0','eng:word:query','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9150);
INSERT INTO sys_menu SELECT 9151,'单词新增',9105,2,'','','','',1,0,'F','0','0','eng:word:add','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9151);
INSERT INTO sys_menu SELECT 9152,'单词修改',9105,3,'','','','',1,0,'F','0','0','eng:word:edit','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9152);
INSERT INTO sys_menu SELECT 9153,'单词删除',9105,4,'','','','',1,0,'F','0','0','eng:word:remove','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9153);
INSERT INTO sys_menu SELECT 9160,'播放新增',9106,1,'','','','',1,0,'F','0','0','eng:playList:add','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9160);
INSERT INTO sys_menu SELECT 9161,'播放修改',9106,2,'','','','',1,0,'F','0','0','eng:playList:edit','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9161);
INSERT INTO sys_menu SELECT 9162,'播放删除',9106,3,'','','','',1,0,'F','0','0','eng:playList:remove','#','admin',NOW(),'',NULL,'' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9162);

-- 将本脚本新增菜单授权给管理员角色；INSERT IGNORE 保证重复执行安全。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,menu_id FROM sys_menu WHERE menu_id BETWEEN 9100 AND 9162;
