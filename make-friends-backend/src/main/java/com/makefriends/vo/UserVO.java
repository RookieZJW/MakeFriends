package com.makefriends.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String phone;

    private String email;

    private String nickname;

    private String avatar;

    private Integer age;

    private Integer gender;

    private LocalDate birthday;

    private Integer height;

    private Integer weight;

    private String city;

    private String occupation;

    private String signature;

    private String hobbies;

    /**
     * 对外展示的在线状态：隐身时返回 false
     */
    private Boolean online;

    /**
     * 仅给自己看：0=离线，1=在线，2=隐身
     */
    private Integer onlineStatus;

    private LocalDateTime createdAt;
}
