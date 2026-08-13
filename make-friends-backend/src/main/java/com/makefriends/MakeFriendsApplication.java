package com.makefriends;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.makefriends.mapper")
public class MakeFriendsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MakeFriendsApplication.class, args);
    }
}
