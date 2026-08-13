package com.makefriends.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.common.Result;
import com.makefriends.service.UserMatchService;
import com.makefriends.vo.UserVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/match")
public class UserMatchController {

    private final UserMatchService userMatchService;

    public UserMatchController(UserMatchService userMatchService) {
        this.userMatchService = userMatchService;
    }

    @PostMapping("/like/{userId}")
    public Result<Map<String, Object>> likeUser(@PathVariable Long userId) {
        boolean matched = userMatchService.likeUser(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("matched", matched);
        result.put("message", matched ? "匹配成功！双方已互相关注" : "已喜欢，等待对方回应");
        return Result.ok(result);
    }

    @DeleteMapping("/unlike/{userId}")
    public Result<Void> unlikeUser(@PathVariable Long userId) {
        userMatchService.unlikeUser(userId);
        return Result.ok();
    }

    @GetMapping("/status/{userId}")
    public Result<Map<String, Integer>> getMatchStatus(@PathVariable Long userId) {
        Integer status = userMatchService.getMatchStatus(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("status", status);
        return Result.ok(result);
    }

    @GetMapping("/my-likes")
    public Result<IPage<UserVO>> getMyLikes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userMatchService.getMyLikes(page, size));
    }

    @GetMapping("/who-likes-me")
    public Result<IPage<UserVO>> getWhoLikesMe(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userMatchService.getWhoLikesMe(page, size));
    }

    @GetMapping("/mutual")
    public Result<IPage<UserVO>> getMutualMatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userMatchService.getMutualMatches(page, size));
    }

    /** 一次性获取三类计数，用于匹配页 Tab 上的小数字 */
    @GetMapping("/counts")
    public Result<Map<String, Long>> getMatchCounts() {
        return Result.ok(userMatchService.getMatchCounts());
    }
}
