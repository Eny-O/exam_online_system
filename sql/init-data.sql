-- 在线考试系统 初始数据脚本
-- 依赖 schema.sql 已执行；默认登录密码均为 123456（BCrypt 加密）
-- 执行：mysql -uroot -p < init-data.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE exam_auth_db;

-- 权限（菜单/按钮/API 标识）
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `type`, `path`, `icon`, `component`, `create_time`, `update_time`) VALUES (100,0,'exam:center','考试中心',1,'/exam','Document','Layout','2026-07-20 15:24:19','2026-07-20 15:24:32'),(101,100,'exam:list:view','考试列表',2,'/exam/list','List','student/Exam/ExamList','2026-07-20 15:26:00','2026-07-20 15:26:04'),(200,0,'score:center','成绩中心',1,'/score','DataAnalysis','Layout','2026-07-20 15:27:01','2026-07-20 15:27:03'),(201,200,'score:query','成绩查询',2,'/score/list','Trophy','student/Exam/ResultView','2026-07-20 15:31:28','2026-07-20 15:31:31'),(300,0,'profile:center','个人中心',2,'/profile','User','Profile/ProfileView','2026-07-20 15:28:55','2026-07-20 15:28:57'),(400,0,'question:manager','题库管理',2,'/teacher/question','Files','teacher/QuestionManagement/QuestionManagement','2026-07-20 15:33:30','2026-07-20 15:33:32'),(500,0,'examtask:management','考试管理',1,'/teacher/exam','EditPen','teacher/ExamTask/ExamTask','2026-07-20 15:35:24','2026-07-20 15:35:26'),(501,500,'my:exam','我的考试',2,'/teacher/examTask/myexam','EditPen','teacher/ExamTask/ExamTask','2026-08-04 09:31:47','2026-08-04 09:31:51'),(600,0,'exampaper:management','试卷管理',2,'/teacher/paper','EditPen','teacher/PaperManagement/PaperManagement','2026-07-28 22:06:55','2026-07-28 22:07:01'),(700,0,'system','系统管理',1,'/system','Setting','Layout','2026-08-17 22:27:18','2026-08-17 22:27:23'),(701,700,'userManage','人员管理',2,'/user/manage','User','admin/userManage/UserManage','2026-08-17 22:27:20','2026-08-17 22:27:26'),(702,700,'sys:permission','权限管理',2,'/system/permission','Lock','admin/permissionManage/PermissionManage','2026-08-17 22:59:04','2026-08-17 22:59:07'),(703,700,'sys:class','班级管理',2,'/system/class','Collection','admin/classManage/ClassManage',NULL,NULL),(800,100,'stu:exam:view','查看考试',4,NULL,NULL,NULL,NULL,NULL),(801,100,'stu:exam:submit','提交试卷',4,NULL,NULL,NULL,NULL,NULL),(802,100,'stu:exam:answer','修改答案',4,NULL,NULL,NULL,NULL,NULL),(810,400,'tch:question:view','查看题目',4,NULL,NULL,NULL,NULL,NULL),(811,400,'tch:question:add','新增题目',4,NULL,NULL,NULL,NULL,NULL),(812,400,'tch:question:edit','编辑题目',4,NULL,NULL,NULL,NULL,NULL),(813,400,'tch:question:delete','删除题目',4,NULL,NULL,NULL,NULL,NULL),(820,600,'tch:paper:view','查看试卷',4,NULL,NULL,NULL,NULL,NULL),(821,600,'tch:paper:add','新增试卷',4,NULL,NULL,NULL,NULL,NULL),(822,600,'tch:paper:edit','编辑试卷',4,NULL,NULL,NULL,NULL,NULL),(823,600,'tch:paper:delete','删除试卷',4,NULL,NULL,NULL,NULL,NULL),(824,600,'tch:paper:status','变更状态',4,NULL,NULL,NULL,NULL,NULL),(830,500,'tch:exam:view','查看考试',4,NULL,NULL,NULL,NULL,NULL),(831,500,'tch:exam:add','新增考试',4,NULL,NULL,NULL,NULL,NULL),(832,500,'tch:exam:edit','编辑考试',4,NULL,NULL,NULL,NULL,NULL),(833,500,'tch:exam:status','变更状态',4,NULL,NULL,NULL,NULL,NULL),(834,500,'tch:exam:delete','删除考试',4,NULL,NULL,NULL,NULL,NULL),(840,0,'tch:grade','阅卷管理',3,'/teacher/grade',NULL,'teacher/OnlineGrading/OnlineGrading',NULL,NULL),(841,840,'tch:grade:view','查看阅卷',4,NULL,NULL,NULL,NULL,NULL),(842,840,'tch:grade:score','批阅打分',4,NULL,NULL,NULL,NULL,NULL),(843,840,'tch:grade:auto','自动批阅',4,NULL,NULL,NULL,NULL,NULL),(850,0,'tch:file:upload','文件上传',4,NULL,NULL,NULL,NULL,NULL),(851,0,'tch:class:view','查看班级',4,NULL,NULL,NULL,NULL,NULL),(900,701,'sys:user:view','查看人员',4,NULL,NULL,NULL,NULL,NULL),(901,701,'sys:user:add','新增人员',4,NULL,NULL,NULL,NULL,NULL),(902,701,'sys:user:edit','编辑人员',4,NULL,NULL,NULL,NULL,NULL),(903,701,'sys:user:status','切换状态',4,NULL,NULL,NULL,NULL,NULL),(904,701,'sys:user:reset','重置密码',4,NULL,NULL,NULL,NULL,NULL),(905,702,'sys:perm:view','查看权限',4,NULL,NULL,NULL,NULL,NULL),(906,702,'sys:perm:add','新增权限',4,NULL,NULL,NULL,NULL,NULL),(907,702,'sys:perm:edit','编辑权限',4,NULL,NULL,NULL,NULL,NULL),(908,702,'sys:perm:delete','删除权限',4,NULL,NULL,NULL,NULL,NULL),(909,700,'sys:role:auth','角色授权',2,'/system/role-auth','UserFilled','admin/roleAuth/RoleAuth',NULL,NULL),(910,703,'sys:class:view','查看班级',4,NULL,NULL,NULL,NULL,NULL),(911,703,'sys:class:add','新增班级',4,NULL,NULL,NULL,NULL,NULL),(912,703,'sys:class:edit','编辑班级',4,NULL,NULL,NULL,NULL,NULL),(913,703,'sys:class:delete','删除班级',4,NULL,NULL,NULL,NULL,NULL),(1000,0,'class:management','班级管理',1,'/teacher/classManagement','School',NULL,NULL,NULL),(1001,1000,'tch:class:mine','我的班级',2,'/teacher/class/my','User','teacher/ClassManage/MyClass',NULL,NULL),(1002,1000,'class:exam:score','班级成绩',2,'/teacher/classExamScore','DataLine','teacher/ClassExamScore/ClassExamScore',NULL,NULL);

-- 角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `create_time`, `update_time`) VALUES (1,'学生','ROLE_STUDENT','2026-07-28 22:45:23','2026-07-28 22:45:23'),(2,'教师','ROLE_TEACHER','2026-07-28 22:45:23','2026-07-28 22:45:23'),(3,'管理员','ROLE_ADMIN','2026-07-28 22:45:23','2026-07-28 22:45:23');

-- 角色-权限关联（学生/教师为既有数据，管理员追加全部权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (1,100),(1,101),(1,200),(1,201),(1,300),(1,800),(1,801),(1,802),(2,400),(2,500),(2,501),(2,600),(2,810),(2,811),(2,812),(2,813),(2,820),(2,821),(2,822),(2,823),(2,824),(2,830),(2,831),(2,832),(2,833),(2,834),(2,840),(2,841),(2,842),(2,843),(2,850),(2,851),(2,1000),(2,1001),(2,1002);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES (3,100),(3,101),(3,200),(3,201),(3,300),(3,400),(3,500),(3,501),(3,600),(3,700),(3,701),(3,702),(3,703),(3,800),(3,801),(3,802),(3,810),(3,811),(3,812),(3,813),(3,820),(3,821),(3,822),(3,823),(3,824),(3,830),(3,831),(3,832),(3,833),(3,834),(3,840),(3,841),(3,842),(3,843),(3,850),(3,851),(3,900),(3,901),(3,902),(3,903),(3,904),(3,905),(3,906),(3,907),(3,908),(3,909),(3,910),(3,911),(3,912),(3,913),(3,1000),(3,1001),(3,1002);

-- 初始班级
INSERT INTO `sys_class` (`id`, `class_name`, `grade`, `deleted`) VALUES (1, '软件工程1班', '2023级', 0);

-- 初始用户：管理员 / 教师 / 学生（密码 123456，BCrypt）
INSERT INTO `sys_user` (`id`, `student_no`, `staff_no`, `username`, `password`, `real_name`, `phone`, `major`, `user_type`, `status`) VALUES
(1, NULL, NULL, 'admin', '$2a$10$54wmAwLAvG8cLmhxJzTQxuNmpFM9xQTlvCSuthktfmS3tC8g3/lxO', '系统管理员', '13800000000', NULL, 3, 1),
(2, NULL, 'T2024001', 'teacher', '$2a$10$54wmAwLAvG8cLmhxJzTQxuNmpFM9xQTlvCSuthktfmS3tC8g3/lxO', '张老师', '13800000001', '计算机科学与技术', 2, 1),
(3, '2024001001', NULL, 'student', '$2a$10$54wmAwLAvG8cLmhxJzTQxuNmpFM9xQTlvCSuthktfmS3tC8g3/lxO', '李同学', '13800000002', '计算机科学与技术', 1, 1);

-- 用户-角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 3), (2, 2), (3, 1);

-- 用户-班级关联（教师任教、学生归属）
INSERT INTO `sys_user_class` (`user_id`, `class_id`, `relation_type`) VALUES (2, 1, 2), (3, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
