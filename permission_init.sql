-- =============================================
-- 权限管理 - 新增 API 级别权限记录
-- 编号规则：学生 800+ / 教师 810+ / 管理员 900+
-- =============================================

-- =====================
-- 一、学生端权限 (800-802)
-- =====================
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(800, 100, 'stu:exam:view',   '查看考试', 4, NULL, NULL, NULL),
(801, 100, 'stu:exam:submit', '提交试卷', 4, NULL, NULL, NULL),
(802, 100, 'stu:exam:answer', '修改答案', 4, NULL, NULL, NULL);

-- =====================
-- 二、教师端权限 (810-833)
-- =====================

-- 题目管理
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(810, 400, 'tch:question:view',   '查看题目', 4, NULL, NULL, NULL),
(811, 400, 'tch:question:add',    '新增题目', 4, NULL, NULL, NULL),
(812, 400, 'tch:question:edit',   '编辑题目', 4, NULL, NULL, NULL),
(813, 400, 'tch:question:delete', '删除题目', 4, NULL, NULL, NULL);

-- 试卷管理
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(820, 600, 'tch:paper:view',   '查看试卷', 4, NULL, NULL, NULL),
(821, 600, 'tch:paper:add',    '新增试卷', 4, NULL, NULL, NULL),
(822, 600, 'tch:paper:edit',   '编辑试卷', 4, NULL, NULL, NULL),
(823, 600, 'tch:paper:delete', '删除试卷', 4, NULL, NULL, NULL),
(824, 600, 'tch:paper:status', '变更状态', 4, NULL, NULL, NULL);

-- 考试管理
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(830, 500, 'tch:exam:view',   '查看考试', 4, NULL, NULL, NULL),
(831, 500, 'tch:exam:add',    '新增考试', 4, NULL, NULL, NULL),
(832, 500, 'tch:exam:edit',   '编辑考试', 4, NULL, NULL, NULL),
(833, 500, 'tch:exam:status', '变更状态', 4, NULL, NULL, NULL);

-- 阅卷管理（新建父级菜单 840）
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(840, 0,   'tch:grade',       '阅卷管理', 2, NULL, NULL, NULL),
(841, 840, 'tch:grade:view',  '查看阅卷', 4, NULL, NULL, NULL),
(842, 840, 'tch:grade:score', '批阅打分', 4, NULL, NULL, NULL),
(843, 840, 'tch:grade:auto',  '自动批阅', 4, NULL, NULL, NULL);

-- 文件上传 & 班级查看
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(850, 0,   'tch:file:upload', '文件上传', 4, NULL, NULL, NULL),
(851, 0,   'tch:class:view',  '查看班级', 4, NULL, NULL, NULL);

-- =====================
-- 三、管理员权限 (900-909)
-- =====================

-- 人员管理按钮权限
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(900, 701, 'sys:user:view',   '查看人员', 4, NULL, NULL, NULL),
(901, 701, 'sys:user:add',    '新增人员', 4, NULL, NULL, NULL),
(902, 701, 'sys:user:edit',   '编辑人员', 4, NULL, NULL, NULL),
(903, 701, 'sys:user:status', '切换状态', 4, NULL, NULL, NULL),
(904, 701, 'sys:user:reset',  '重置密码', 4, NULL, NULL, NULL);

-- 权限管理按钮权限
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, type, path, icon, component) VALUES
(905, 702, 'sys:perm:view',   '查看权限', 4, NULL, NULL, NULL),
(906, 702, 'sys:perm:add',    '新增权限', 4, NULL, NULL, NULL),
(907, 702, 'sys:perm:edit',   '编辑权限', 4, NULL, NULL, NULL),
(908, 702, 'sys:perm:delete', '删除权限', 4, NULL, NULL, NULL);

-- =============================================
-- 四、角色-权限关联
-- =============================================

-- 学生角色 (role_id=1)：关联学生端 API 权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 800), (1, 801), (1, 802);

-- 教师角色 (role_id=2)：关联教师端 API 权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 810), (2, 811), (2, 812), (2, 813),
(2, 820), (2, 821), (2, 822), (2, 823), (2, 824),
(2, 830), (2, 831), (2, 832), (2, 833),
(2, 841), (2, 842), (2, 843),
(2, 850), (2, 851);

-- 管理员角色 (role_id=3)：无需插入
-- 管理员通过 SQL UNION 自动获得全部权限
