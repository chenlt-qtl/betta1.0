-- ----------------------------
-- 卡管理菜单权限
-- ----------------------------

-- 一级菜单：卡管理
INSERT INTO sys_menu VALUES('2000', '卡管理', '0', '5', 'card', NULL, '', '', 1, 0, 'M', '0', '0', '', 'credit-card', 'admin', sysdate(), '', NULL, '卡管理目录');

-- 二级菜单
INSERT INTO sys_menu VALUES('2001', '加卡', '2000', '1', 'index', 'card/index', '', '', 1, 0, 'C', '0', '0', 'system:card:operate', 'edit', 'admin', sysdate(), '', NULL, '加卡菜单');
INSERT INTO sys_menu VALUES('2002', '账户管理', '2000', '2', 'account', 'card/account', '', '', 1, 0, 'C', '0', '0', 'system:card:account:list', 'user', 'admin', sysdate(), '', NULL, '账户管理菜单');
INSERT INTO sys_menu VALUES('2003', '历史记录', '2000', '3', 'history', 'card/history', '', '', 1, 0, 'C', '0', '0', 'system:card:history:list', 'time', 'admin', sysdate(), '', NULL, '历史记录菜单');
INSERT INTO sys_menu VALUES('2004', '预设项', '2000', '4', 'item', 'card/item', '', '', 1, 0, 'C', '0', '0', 'system:card:item:list', 'list', 'admin', sysdate(), '', NULL, '预设项菜单');

-- 加卡按钮权限
INSERT INTO sys_menu VALUES('2010', '加扣卡操作', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:card:operate', '#', 'admin', sysdate(), '', NULL, '');
