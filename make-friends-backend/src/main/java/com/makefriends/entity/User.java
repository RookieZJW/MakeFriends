package com.makefriends.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String password;

    private String email;

    private String nickname;

    private String avatar;

    private Integer age;

    private Integer gender;

    private LocalDate birthday;

    private Integer status;

    private Integer height;

    private Integer weight;

    private String city;

    private String occupation;

    private String signature;

    private String hobbies;

    /**
     * 在线状态：0=离线，1=在线，2=隐身
     */
    private Integer onlineStatus;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
