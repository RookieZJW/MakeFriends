package com.makefriends.controller;

import com.makefriends.common.Result;
import com.makefriends.entity.HobbyDict;
import com.makefriends.entity.OccupationDict;
import com.makefriends.service.DictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /** 兴趣爱好列表 */
    @GetMapping("/hobbies")
    public Result<List<HobbyDict>> listHobbies() {
        return Result.ok(dictService.listHobbies());
    }

    /** 职业列表 */
    @GetMapping("/occupations")
    public Result<List<OccupationDict>> listOccupations() {
        return Result.ok(dictService.listOccupations());
    }

    /** 一次性拉取两个字典（减少一次请求） */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAll() {
        return Result.ok(dictService.getAllDicts());
    }
}
