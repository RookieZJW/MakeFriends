package com.makefriends.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DynamicVO {

    private Long id;

    private Long userId;

    private String content;

    private String images;

    private Integer likeCount;

    private Integer commentCount;

    private LocalDateTime createdAt;

    private String nickname;

    private String avatar;

    private Boolean liked;
}
