package com.makefriends.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.makefriends.entity.HobbyDict;
import com.makefriends.entity.OccupationDict;
import com.makefriends.mapper.HobbyDictMapper;
import com.makefriends.mapper.OccupationDictMapper;
import com.makefriends.service.DictService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictServiceImpl implements DictService {

    private final HobbyDictMapper hobbyDictMapper;
    private final OccupationDictMapper occupationDictMapper;

    public DictServiceImpl(HobbyDictMapper hobbyDictMapper, OccupationDictMapper occupationDictMapper) {
        this.hobbyDictMapper = hobbyDictMapper;
        this.occupationDictMapper = occupationDictMapper;
    }

    @Override
    public List<HobbyDict> listHobbies() {
        try {
            return hobbyDictMapper.selectList(
                    new LambdaQueryWrapper<HobbyDict>()
                            .eq(HobbyDict::getStatus, 1)
                            .orderByAsc(HobbyDict::getSort)
                            .orderByAsc(HobbyDict::getId));
        } catch (Exception e) {
            // 数据库表未初始化时降级：前端就不会 500，但会显示空数组
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public List<OccupationDict> listOccupations() {
        try {
            return occupationDictMapper.selectList(
                    new LambdaQueryWrapper<OccupationDict>()
                            .eq(OccupationDict::getStatus, 1)
                            .orderByAsc(OccupationDict::getSort)
                            .orderByAsc(OccupationDict::getId));
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getAllDicts() {
        Map<String, Object> result = new HashMap<>();
        result.put("hobbies", listHobbies());
        result.put("occupations", listOccupations());
        return result;
    }
}
