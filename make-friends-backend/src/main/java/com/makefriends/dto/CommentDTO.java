package com.makefriends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentDTO {

    @NotNull(message = "动态ID不能为空")
    private Long dynamicId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;
}
