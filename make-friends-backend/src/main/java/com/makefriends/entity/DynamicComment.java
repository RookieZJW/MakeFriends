package com.makefriends.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dynamic_comments")
public class DynamicComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dynamicId;

    private Long userId;

    private String content;

    private Long parentId;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
