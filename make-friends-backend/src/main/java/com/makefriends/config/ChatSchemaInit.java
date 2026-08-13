package com.makefriends.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatSchemaInit {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            // chat_sessions: user1_deleted / user2_deleted
            addColumnIfMissing("chat_sessions", "user1_deleted",
                    "ALTER TABLE chat_sessions ADD COLUMN user1_deleted INT DEFAULT 0 COMMENT 'user1是否删除该会话 1=已删除'");
            addColumnIfMissing("chat_sessions", "user2_deleted",
                    "ALTER TABLE chat_sessions ADD COLUMN user2_deleted INT DEFAULT 0 COMMENT 'user2是否删除该会话 1=已删除'");
            try {
                jdbcTemplate.update("UPDATE chat_sessions SET user1_deleted = 0 WHERE user1_deleted IS NULL");
                jdbcTemplate.update("UPDATE chat_sessions SET user2_deleted = 0 WHERE user2_deleted IS NULL");
            } catch (Exception ignore) {
            }

            // users: online_status / last_active_at
            addColumnIfMissing("users", "online_status",
                    "ALTER TABLE users ADD COLUMN online_status INT DEFAULT 0 COMMENT '在线状态 0=离线 1=在线 2=隐身'");
            addColumnIfMissing("users", "last_active_at",
                    "ALTER TABLE users ADD COLUMN last_active_at DATETIME NULL COMMENT '最后活跃时间'");
        } catch (Exception e) {
            // 忽略初始化失败（表不存在等），应用启动不受影响
        }
    }

    private void addColumnIfMissing(String table, String column, String addSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count == null || count == 0) {
                jdbcTemplate.execute(addSql);
            }
        } catch (Exception e) {
            // 列可能已存在
        }
    }
}
