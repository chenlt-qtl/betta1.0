-- 修复生产环境文章关卡地图因隐藏菜单或角色授权缺失而进入 404。

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,
 menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT 9116,'文章关卡',9100,14,'study/levels/:articleId','eng/study/levelMap','','',1,0,'C','1','0',
 'eng:study:challenge','star','admin',NOW(),'文章新词关卡地图'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE menu_id=9116);

-- 新地图路由继承原文章闯关路由的角色范围，避免向无闯关权限角色扩权。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT role_id,9116 FROM sys_role_menu WHERE menu_id=9111;

-- 管理员不依赖旧菜单授权，也应始终能够访问隐藏地图路由。
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES(1,9116);
