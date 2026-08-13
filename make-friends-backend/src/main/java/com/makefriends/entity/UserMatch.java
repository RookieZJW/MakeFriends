package com.makefriends.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_matches")
public class UserMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private Integer matchType;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
