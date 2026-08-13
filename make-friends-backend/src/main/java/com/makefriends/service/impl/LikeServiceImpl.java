package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.makefriends.entity.DynamicLike;
import com.makefriends.entity.UserDynamic;
import com.makefriends.mapper.DynamicLikeMapper;
import com.makefriends.mapper.UserDynamicMapper;
import com.makefriends.service.LikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl implements LikeService {

    private final DynamicLikeMapper likeMapper;
    private final UserDynamicMapper dynamicMapper;

    public LikeServiceImpl(DynamicLikeMapper likeMapper, UserDynamicMapper dynamicMapper) {
        this.likeMapper = likeMapper;
        this.dynamicMapper = dynamicMapper;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long dynamicId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDynamic dynamic = dynamicMapper.selectById(dynamicId);
        if (dynamic == null || (dynamic.getStatus() != null && dynamic.getStatus() != 1)) {
            throw new IllegalArgumentException("动态不存在");
        }

        DynamicLike existing = likeMapper.selectOne(
                new LambdaQueryWrapper<DynamicLike>()
                        .eq(DynamicLike::getDynamicId, dynamicId)
                        .eq(DynamicLike::getUserId, userId)
                        .last("LIMIT 1"));

        boolean nowLiked;
        if (existing == null) {
            DynamicLike like = new DynamicLike();
            like.setDynamicId(dynamicId);
            like.setUserId(userId);
            like.setStatus(1);
            likeMapper.insert(like);
            nowLiked = true;
        } else {
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                existing.setStatus(0);
                nowLiked = false;
            } else {
                existing.setStatus(1);
                nowLiked = true;
            }
            likeMapper.updateById(existing);
        }

        Long count = likeMapper.selectCount(
                new LambdaQueryWrapper<DynamicLike>()
                        .eq(DynamicLike::getDynamicId, dynamicId)
                        .eq(DynamicLike::getStatus, 1));
        dynamic.setLikeCount(count.intValue());
        dynamicMapper.updateById(dynamic);

        return nowLiked;
    }

    @Override
    public boolean checkLiked(Long dynamicId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = likeMapper.selectCount(
                new LambdaQueryWrapper<DynamicLike>()
                        .eq(DynamicLike::getDynamicId, dynamicId)
                        .eq(DynamicLike::getUserId, userId)
                        .eq(DynamicLike::getStatus, 1));
        return count > 0;
    }
}
