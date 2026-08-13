package com.makefriends.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.common.Result;
import com.makefriends.dto.DynamicPublishDTO;
import com.makefriends.service.DynamicService;
import com.makefriends.vo.DynamicVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dynamic")
public class DynamicController {

    private final DynamicService dynamicService;

    public DynamicController(DynamicService dynamicService) {
        this.dynamicService = dynamicService;
    }

    @PostMapping
    public Result<DynamicVO> publish(@Valid @RequestBody DynamicPublishDTO dto) {
        return Result.ok(dynamicService.publish(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dynamicService.delete(id);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<IPage<DynamicVO>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(dynamicService.getList(page, size));
    }

    @GetMapping("/{id}")
    public Result<DynamicVO> getDetail(@PathVariable Long id) {
        return Result.ok(dynamicService.getDetail(id));
    }

    @GetMapping("/my")
    public Result<IPage<DynamicVO>> getMyDynamics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(dynamicService.getMyDynamics(page, size));
    }

    @GetMapping("/user/{userId}")
    public Result<IPage<DynamicVO>> getUserDynamics(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(dynamicService.getUserDynamics(userId, page, size));
    }
}
