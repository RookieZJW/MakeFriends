package com.makefriends.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.common.Result;
import com.makefriends.dto.UserUpdateDTO;
import com.makefriends.service.UserService;
import com.makefriends.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserVO> getMyInfo() {
        return Result.ok(userService.getMyInfo());
    }

    @PutMapping("/me")
    public Result<UserVO> updateMyInfo(@Valid @RequestBody UserUpdateDTO dto) {
        return Result.ok(userService.updateMyInfo(dto));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @GetMapping("/list")
    public Result<IPage<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String occupation,
            @RequestParam(required = false) String hobby) {
        return Result.ok(userService.getUserList(page, size, gender, city, minAge, maxAge, occupation, hobby));
    }

    /**
     * 心跳：前端每 30 秒调一次，标记我仍在线
     */
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat() {
        userService.heartbeat();
        return Result.ok();
    }

    /**
     * 设置在线状态：status=1 在线, status=2 隐身
     */
    @PostMapping("/online-status")
    public Result<Void> setOnlineStatus(@RequestParam Integer status) {
        userService.setOnlineStatus(status);
        return Result.ok();
    }
}
