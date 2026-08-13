package com.makefriends.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DynamicPublishDTO {

    @NotBlank(message = "内容不能为空")
    private String content;

    private String images;
}
