package com.makefriends.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {

    private Long id;

    private Long userId;

    private String nickname;

    private String avatar;

    private String lastMessage;

    private Integer unreadCount;

    private LocalDateTime lastMsgTime;

    /**
     * 对方是否在线（隐身视为离线）
     */
    private Boolean online;
}
