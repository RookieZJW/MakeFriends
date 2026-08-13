-- ============================================================
-- MakeFriends 社交平台 - 数据库表结构完整导出
-- 说明：本文件通过 SHOW CREATE TABLE 从真实数据库导出，含 9 张核心表。
--       外键约束、索引、自增起始值均与线上一致。
--       字典数据（爱好 / 职业）请另执行 003_hobby_occupation_dict.sql
-- 导出时间：2026-08-13
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户基础信息表
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号（登录账号）',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱（选填）',
  `nickname` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `avatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像（例：/upload/图片.jpg）',
  `age` int DEFAULT NULL COMMENT '年龄',
  `gender` tinyint NOT NULL COMMENT '性别：1-男，2-女，0-未设置',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：1-正常，0-禁用，2-注销',
  `height` smallint DEFAULT NULL COMMENT '身高（cm）',
  `weight` smallint DEFAULT NULL COMMENT '体重（kg）',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所在城市',
  `occupation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '职业',
  `signature` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个性签名',
  `hobbies` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '兴趣爱好（用逗号分隔，如：旅游,美食,运动）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `online_status` int DEFAULT '0' COMMENT '在线状态 0=离线 1=在线 2=隐身',
  `last_active_at` datetime DEFAULT NULL COMMENT '最后活跃时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_phone` (`phone`) USING BTREE,
  KEY `idx_city` (`city`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户基础信息表';

-- ----------------------------
-- 2. 用户匹配 / 喜欢 / 互关记录表
-- ----------------------------
DROP TABLE IF EXISTS `user_matches`;
CREATE TABLE `user_matches` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '匹配记录ID',
  `from_user_id` bigint unsigned NOT NULL COMMENT '主动方用户ID',
  `to_user_id` bigint unsigned NOT NULL COMMENT '被动方用户ID',
  `match_type` tinyint NOT NULL COMMENT '匹配类型：1-喜欢，2-取消喜欢，3-互关',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-有效，0-无效',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_from_to` (`from_user_id`,`to_user_id`) USING BTREE,
  KEY `idx_from_user_id` (`from_user_id`) USING BTREE,
  KEY `idx_to_user_id` (`to_user_id`) USING BTREE,
  CONSTRAINT `user_matches_ibfk_1` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_matches_ibfk_2` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户匹配记录表';

-- ----------------------------
-- 3. 聊天会话表
-- ----------------------------
DROP TABLE IF EXISTS `chat_sessions`;
CREATE TABLE `chat_sessions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user1_id` bigint unsigned NOT NULL COMMENT '用户1ID',
  `user2_id` bigint unsigned NOT NULL COMMENT '用户2ID',
  `last_msg_id` bigint unsigned DEFAULT NULL COMMENT '最后一条消息ID',
  `unread_count` int unsigned DEFAULT '0' COMMENT '未读消息数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，0-拉黑',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `user1_deleted` int DEFAULT '0' COMMENT 'user1是否删除该会话 1=已删除',
  `user2_deleted` int DEFAULT '0' COMMENT 'user2是否删除该会话 1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user1_user2` (`user1_id`,`user2_id`) USING BTREE,
  KEY `idx_user1_id` (`user1_id`) USING BTREE,
  KEY `idx_user2_id` (`user2_id`) USING BTREE,
  CONSTRAINT `chat_sessions_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chat_sessions_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='聊天会话表';

-- ----------------------------
-- 4. 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint unsigned NOT NULL COMMENT '关联会话ID',
  `sender_id` bigint unsigned NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint unsigned NOT NULL COMMENT '接收者ID',
  `msg_type` tinyint NOT NULL COMMENT '消息类型：1-文本，2-图片，3-语音，4-视频',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容（文本/URL）',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息日期',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_session_id` (`session_id`) USING BTREE,
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  KEY `receiver_id` (`receiver_id`) USING BTREE,
  CONSTRAINT `chat_messages_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chat_messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chat_messages_ibfk_3` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';

-- ----------------------------
-- 5. 用户动态表
-- ----------------------------
DROP TABLE IF EXISTS `user_dynamics`;
CREATE TABLE `user_dynamics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` bigint unsigned NOT NULL COMMENT '发布者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '动态内容',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片URL（逗号分隔）',
  `like_count` int unsigned DEFAULT '0' COMMENT '点赞数',
  `comment_count` int unsigned DEFAULT '0' COMMENT '评论数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，0-删除，2-审核中',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  CONSTRAINT `user_dynamics_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户动态表';

-- ----------------------------
-- 6. 动态评论表（支持 parent_id 嵌套回复）
-- ----------------------------
DROP TABLE IF EXISTS `dynamic_comments`;
CREATE TABLE `dynamic_comments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `dynamic_id` bigint unsigned NOT NULL COMMENT '动态ID',
  `user_id` bigint unsigned NOT NULL COMMENT '评论用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `parent_id` bigint unsigned DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，0-删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '评论更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dynamic_id` (`dynamic_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  CONSTRAINT `dynamic_comments_ibfk_1` FOREIGN KEY (`dynamic_id`) REFERENCES `user_dynamics` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dynamic_comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dynamic_comments_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `dynamic_comments` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='动态评论表';

-- ----------------------------
-- 7. 动态点赞表（uk_dynamic_user 去重）
-- ----------------------------
DROP TABLE IF EXISTS `dynamic_likes`;
CREATE TABLE `dynamic_likes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `dynamic_id` bigint unsigned NOT NULL COMMENT '动态ID',
  `user_id` bigint unsigned NOT NULL COMMENT '点赞用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-点赞，0-取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dynamic_user` (`dynamic_id`,`user_id`) USING BTREE,
  KEY `idx_dynamic_id` (`dynamic_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  CONSTRAINT `dynamic_likes_ibfk_1` FOREIGN KEY (`dynamic_id`) REFERENCES `user_dynamics` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `dynamic_likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='动态点赞表';

-- ----------------------------
-- 8. 兴趣爱好字典表（数据见 003_hobby_occupation_dict.sql）
-- ----------------------------
DROP TABLE IF EXISTS `hobby_dict`;
CREATE TABLE `hobby_dict` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '爱好名称，如：摄影',
  `icon` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可选图标/表情',
  `category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类：运动/艺术/生活/学习/娱乐/科技/户外/美食/旅行',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序：小的靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_category` (`category`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兴趣爱好字典表';

-- ----------------------------
-- 9. 职业字典表（数据见 003_hobby_occupation_dict.sql）
-- ----------------------------
DROP TABLE IF EXISTS `occupation_dict`;
CREATE TABLE `occupation_dict` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职业名称，如：设计师',
  `icon` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可选图标/表情',
  `category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '行业分类：互联网/金融/医疗/教育/制造/文化/服务/政府/学生/自由职业',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序：小的靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_category` (`category`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职业字典表';

SET FOREIGN_KEY_CHECKS = 1;
