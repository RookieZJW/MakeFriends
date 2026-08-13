-- =====================================================
-- MakeFriends 高并发优化：关键复合覆盖索引
-- MySQL 8.0 / utf8mb4  ·  执行前 USE make_friends
-- 若返回 "Duplicate key name / Duplicate column name" 可直接忽略
-- =====================================================
-- 1. users.phone UK（防暴力注册 + 登录定位行）已创建 uk_users_phone，保持现状

-- 2. user_matches: 原表使用 from_user_id / to_user_id (不是 user_id/target_user_id)
--    已有单列 idx_from_user_id / idx_to_user_id + 唯一 UK uk_from_to
--    补: "喜欢我的列表" 查询 WHERE to_user_id=? AND match_type=? 复合索引
ALTER TABLE user_matches ADD INDEX idx_to_type (to_user_id, match_type);

-- 3. chat_messages: 分页历史消息 WHERE session_id=? ORDER BY created_at DESC LIMIT ?,?
--    已有单列 idx_session_id / idx_created_at / receiver_id / idx_sender_receiver
--    补: 复合覆盖 (session_id, created_at DESC)
ALTER TABLE chat_messages ADD INDEX idx_session_created (session_id, created_at DESC);
--    补: 会话未读计数 WHERE session_id=? AND receiver_id=? AND is_read=0
ALTER TABLE chat_messages ADD INDEX idx_session_receiver_read (session_id, receiver_id, is_read);

-- 4. user_dynamics: 用户主页 + 广场排序
--    已有单列 idx_user_id / idx_created_at
--    补: 复合 (user_id, created_at DESC) 替代 2 个单列联合成本
ALTER TABLE user_dynamics ADD INDEX idx_user_created (user_id, created_at DESC);
--    补: 广场按状态+时间 (status, created_at DESC)  审核中/正常分区列表
ALTER TABLE user_dynamics ADD INDEX idx_status_created (status, created_at DESC);

-- 5. dynamic_comments: 动态详情页评论区 WHERE dynamic_id=? ORDER BY created_at ASC
--    已有 idx_dynamic_id
ALTER TABLE dynamic_comments ADD INDEX idx_dynamic_created (dynamic_id, created_at);