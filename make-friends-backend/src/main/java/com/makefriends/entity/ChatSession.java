package com.makefriends.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_sessions")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long user1Id;

    private Long user2Id;

    private Long lastMsgId;

    private Integer unreadCount;

    private Integer status;

    /**
     * user1 是否删除了该会话（1=已删除，0=未删除）
     */
    private Integer user1Deleted;

    /**
     * user2 是否删除了该会话（1=已删除，0=未删除）
     */
    private Integer user2Deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
