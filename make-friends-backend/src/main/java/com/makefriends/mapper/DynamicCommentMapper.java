package com.makefriends.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.makefriends.entity.DynamicComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DynamicCommentMapper extends BaseMapper<DynamicComment> {
}
