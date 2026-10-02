-- 恢复独立生词本入口；固定主键及 NOT EXISTS 保证脚本可重复执行。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9114,'生词本',9100,5,'word-book','eng/word/wordBook','','',1,0,'C','0','0','eng:score:list','bookmark','admin',NOW(),'用户生词本'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9114);

-- 幂等授予管理员角色生词本菜单权限。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT 1,9114 FROM sys_menu WHERE menu_id=9114;
