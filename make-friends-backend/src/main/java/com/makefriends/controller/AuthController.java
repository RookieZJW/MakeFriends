package com.makefriends.controller;

import com.makefriends.common.Result;
import com.makefriends.dto.LoginDTO;
import com.makefriends.dto.RegisterDTO;
import com.makefriends.service.UserService;
import com.makefriends.vo.LoginVO;
import com.makefriends.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }
}
