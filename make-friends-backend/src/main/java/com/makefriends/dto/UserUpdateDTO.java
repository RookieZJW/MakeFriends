package com.makefriends.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateDTO {

    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    private String avatar;

    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    private Integer age;

    private Integer gender;

    private LocalDate birthday;

    private Integer height;

    private Integer weight;

    @Size(max = 50, message = "城市长度不能超过50个字符")
    private String city;

    @Size(max = 50, message = "职业长度不能超过50个字符")
    private String occupation;

    @Size(max = 200, message = "个性签名长度不能超过200个字符")
    private String signature;

    @Size(max = 255, message = "爱好长度不能超过255个字符")
    private String hobbies;

    private String phone;

    private String password;

    private Integer status;
}
