package com.makefriends.controller;

import com.makefriends.common.Result;
import com.makefriends.service.LikeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/toggle/{dynamicId}")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long dynamicId) {
        boolean liked = likeService.toggleLike(dynamicId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return Result.ok(result);
    }

    @GetMapping("/check/{dynamicId}")
    public Result<Map<String, Boolean>> checkLiked(@PathVariable Long dynamicId) {
        boolean liked = likeService.checkLiked(dynamicId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("liked", liked);
        return Result.ok(result);
    }
}
