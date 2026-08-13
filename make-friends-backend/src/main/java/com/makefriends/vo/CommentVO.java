package com.makefriends.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long dynamicId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private String replyToNickname;
    private LocalDateTime createdAt;
    private List<CommentVO> children;
}

