-- 在线考试系统 建库建表脚本
-- 包含两套 schema：exam_auth_db（认证/权限/人员）与 exam_core_db（考试/试卷/题库）
-- 执行：mysql -uroot -p < schema.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS exam_auth_db DEFAULT CHARACTER SET utf8mb3 COLLATE utf8mb3_bin;
USE exam_auth_db;

DROP TABLE IF EXISTS `sys_class`;
CREATE TABLE `sys_class` (
  `id` int NOT NULL AUTO_INCREMENT,
  `class_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '班级名称',
  `grade` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '年级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT '0' COMMENT '0-启用；停用后写为该行 id',
  `remark` varchar(200) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_name` (`class_name`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='班级表';

DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parent_id` int DEFAULT NULL COMMENT '父级ID',
  `perm_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '权限标识: exam:create, grade:view',
  `perm_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '权限名称',
  `type` tinyint DEFAULT NULL COMMENT '类型 (1:目录,2.:菜单, 3:按钮, 4:API)',
  `path` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '前端路由路径',
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图标名称',
  `component` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '前端组件路径 ',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_perm_code` (`perm_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2004 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='权限表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '角色编码: ROLE_STUDENT, ROLE_TEACHER, ROLE_ADMIN',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='角色表';

DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='角色权限关联表';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '学号：可为空，学生专属。',
  `staff_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '工号：可为空，教职工专属。',
  `username` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '加密密码(如BCrypt)',
  `real_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `major` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '专业',
  `user_type` tinyint NOT NULL COMMENT '1-学生，2-教职工，3-管理员',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='系统用户表';

DROP TABLE IF EXISTS `sys_user_class`;
CREATE TABLE `sys_user_class` (
  `user_id` int NOT NULL COMMENT '用户ID',
  `class_id` int NOT NULL COMMENT '班级ID',
  `relation_type` tinyint NOT NULL COMMENT '关系类型: 1-学生归属, 2-教师任教',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `student_unique` int GENERATED ALWAYS AS ((case when (`relation_type` = 1) then `user_id` else NULL end)) STORED COMMENT '学生唯一性辅助列, 教师行为NULL',
  PRIMARY KEY (`user_id`,`class_id`,`relation_type`),
  UNIQUE KEY `uk_student_class` (`student_unique`),
  KEY `idx_user_type` (`user_id`,`relation_type`),
  KEY `idx_class_type` (`class_id`,`relation_type`),
  CONSTRAINT `fk_uc_class` FOREIGN KEY (`class_id`) REFERENCES `sys_class` (`id`),
  CONSTRAINT `fk_uc_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户-班级关联表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户角色关联表';

CREATE DATABASE IF NOT EXISTS exam_core_db DEFAULT CHARACTER SET utf8mb3 COLLATE utf8mb3_bin;
USE exam_core_db;

DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '考试名称',
  `paper_id` int DEFAULT NULL COMMENT '关联试卷id\r\n',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `duration` int NOT NULL COMMENT '考试时长(分钟)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-未发布,1-已发布,2-未开始,3-进行中,4-待批阅,5-批阅完成',
  `creator_id` int DEFAULT NULL COMMENT '创建者id',
  `updater_Id` int DEFAULT NULL COMMENT '更新者id',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_paper` (`paper_id`),
  KEY `idx_status` (`status`),
  KEY `idx_creator` (`creator_id`),
  CONSTRAINT `fk_exam_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='考试表';

DROP TABLE IF EXISTS `exam_class`;
CREATE TABLE `exam_class` (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int DEFAULT NULL COMMENT '考试id',
  `class_id` int DEFAULT NULL COMMENT '班级id',
  `creator_id` int DEFAULT NULL COMMENT '创建者id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_class` (`exam_id`,`class_id`),
  KEY `idx_exam` (`exam_id`),
  CONSTRAINT `fk_ec_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='考试-班级关联表';

DROP TABLE IF EXISTS `exam_paper_snapshot`;
CREATE TABLE `exam_paper_snapshot` (
  `exam_id` int NOT NULL COMMENT '考试ID',
  `paper_id` int NOT NULL COMMENT '试卷ID',
  `content` longtext COLLATE utf8mb3_bin NOT NULL COMMENT '快照内容JSON',
  `start_time` datetime DEFAULT NULL COMMENT '发布时刻冻结的开考时间',
  `duration` int DEFAULT NULL COMMENT '发布时刻冻结的考试时长(分钟)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='考试试卷快照表';

DROP TABLE IF EXISTS `exam_student`;
CREATE TABLE `exam_student` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exam_id` int NOT NULL COMMENT '考试ID',
  `student_id` int NOT NULL COMMENT '学生ID',
  `status` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'pending' COMMENT '学生考试状态: pending-待考试,submitted-已交卷,grade-已批阅,absent-缺考',
  `objective_score` decimal(5,1) DEFAULT NULL COMMENT '客观题分数',
  `subjective_score` decimal(5,1) DEFAULT NULL COMMENT '主观题分数',
  `total_score` decimal(5,1) DEFAULT NULL COMMENT '总分',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `creator_id` int DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_student` (`exam_id`,`student_id`),
  KEY `idx_exam_id` (`exam_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='考试-学生关联表';

DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '试卷名称',
  `total_score` decimal(5,1) NOT NULL COMMENT '总分',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '试卷描述',
  `creator_id` int NOT NULL COMMENT '创建者的id\r\n',
  `updater_id` int DEFAULT NULL COMMENT '修改者的id',
  `status` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT为草稿, PENDING_ENABLE为待启用, ALREADY_ENABLE为已启用',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号,控制并发修改异常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='试卷表';

DROP TABLE IF EXISTS `paper_question`;
CREATE TABLE `paper_question` (
  `id` int NOT NULL AUTO_INCREMENT,
  `paper_id` int DEFAULT NULL,
  `question_id` int DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '题目顺序',
  `score` decimal(5,1) NOT NULL COMMENT '该题在本试卷中的分值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_paper` (`paper_id`),
  KEY `fk_pq_question` (`question_id`),
  CONSTRAINT `fk_pq_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pq_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='试卷-题目关联表';

DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '题干内容',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '题目类型: single-单选 multiple-多选 judge-判断 essay-简答',
  `difficulty` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '难度: easy-medium-hard',
  `knowledge_point` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '知识点',
  `answer` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '正确答案(客观题存选项标签如A;判断题存true/false;简答题存参考答案)',
  `analysis` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '解析',
  `creator_id` int DEFAULT NULL COMMENT '创建者ID',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='题库表';

DROP TABLE IF EXISTS `question_option`;
CREATE TABLE `question_option` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int DEFAULT NULL,
  `label` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '选项标签: A/B/C/D/E/F/G/H',
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '选项内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question` (`question_id`),
  CONSTRAINT `fk_option_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=232 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='题目选项表';

DROP TABLE IF EXISTS `student_answer`;
CREATE TABLE `student_answer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NOT NULL,
  `student_id` int NOT NULL,
  `question_id` int NOT NULL,
  `student_answer` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '学生作答内容',
  `is_correct` tinyint(1) DEFAULT NULL COMMENT '是否正确: 1-正确 0-错误 NULL-未评判(主观题) 2-半对(主观题)',
  `score` decimal(5,1) DEFAULT NULL COMMENT '得分',
  `full_score` decimal(5,1) NOT NULL COMMENT '满分',
  `question_type` varchar(20) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '题目类型: single/multiple/judge/essay',
  `teacher_comment` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin COMMENT '教师批注(主观题)',
  `grader_id` int DEFAULT NULL,
  `grade_time` datetime DEFAULT NULL COMMENT '批阅时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_exam_student` (`exam_id`,`student_id`) USING BTREE,
  KEY `idx_question` (`question_id`) USING BTREE,
  KEY `fk_sa_student` (`student_id`) USING BTREE,
  CONSTRAINT `fk_sa_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sa_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=226 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC COMMENT='学生答题记录表';

SET FOREIGN_KEY_CHECKS = 1;
