package com.makefriends.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hobby_dict")
public class HobbyDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String icon;

    private String category;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
