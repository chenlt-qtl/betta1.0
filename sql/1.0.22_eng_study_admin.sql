-- 管理员积分管理菜单；固定主键与 NOT EXISTS/INSERT IGNORE 共同保证脚本可重复执行。
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9115,'积分管理',9101,6,'score-admin','eng/study/scoreAdmin','','',1,0,'C','0','0','eng:study:score:admin','chart','admin',NOW(),'管理员查看全员积分和积分历史'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=9115);

-- 仅默认授权超级管理员角色，重复执行时不会产生重复关系。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES (1,9115);