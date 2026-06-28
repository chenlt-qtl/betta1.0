-- ----------------------------
-- 笔记菜单权限
-- ----------------------------

-- 二级菜单：笔记，挂在系统工具下
INSERT INTO sys_menu VALUES('2100', '笔记', '0', '4', 'note', 'note/index', '', '', 1, 0, 'C', '0', '0', 'system:note:list', 'documentation', 'admin', sysdate(), '', NULL, 'Markdown 笔记');

-- 笔记按钮权限
INSERT INTO sys_menu VALUES('2101', '笔记查询', '2100', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2102', '笔记新增', '2100', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2103', '笔记修改', '2100', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2104', '笔记删除', '2100', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2105', '图片上传', '2100', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:upload', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2106', '笔记下载', '2100', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'system:note:download', '#', 'admin', sysdate(), '', NULL, '');
