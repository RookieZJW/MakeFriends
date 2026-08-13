package com.makefriends.service;

import com.makefriends.entity.HobbyDict;
import com.makefriends.entity.OccupationDict;

import java.util.List;
import java.util.Map;

public interface DictService {

    /** 获取所有启用的兴趣爱好字典（按 sort + id 升序） */
    List<HobbyDict> listHobbies();

    /** 获取所有启用的职业字典（按 sort + id 升序） */
    List<OccupationDict> listOccupations();

    /** 一次性返回两类字典，方便前端一次性加载 */
    Map<String, Object> getAllDicts();
}
