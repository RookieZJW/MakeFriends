package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.makefriends.dto.DynamicPublishDTO;
import com.makefriends.entity.DynamicLike;
import com.makefriends.entity.User;
import com.makefriends.entity.UserDynamic;
import com.makefriends.mapper.DynamicLikeMapper;
import com.makefriends.mapper.UserDynamicMapper;
import com.makefriends.mapper.UserMapper;
import com.makefriends.service.DynamicService;
import com.makefriends.vo.DynamicVO;
import org.springframework.beans.BeanUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DynamicServiceImpl implements DynamicService {

    private final UserDynamicMapper dynamicMapper;
    private final UserMapper userMapper;
    private final DynamicLikeMapper likeMapper;

    public DynamicServiceImpl(UserDynamicMapper dynamicMapper, UserMapper userMapper, DynamicLikeMapper likeMapper) {
        this.dynamicMapper = dynamicMapper;
        this.userMapper = userMapper;
        this.likeMapper = likeMapper;
    }

    @Override
    @CircuitBreaker(name = "user-dynamic-db")
    @Retry(name = "mysql-retry")
    public DynamicVO publish(DynamicPublishDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDynamic dynamic = new UserDynamic();
        dynamic.setUserId(userId);
        dynamic.setContent(dto.getContent());
        dynamic.setImages(dto.getImages());
        dynamic.setLikeCount(0);
        dynamic.setCommentCount(0);
        dynamic.setStatus(1);
        dynamicMapper.insert(dynamic);
        return toVO(dynamic, null, null);
    }

    @Override
    public boolean delete(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDynamic dynamic = dynamicMapper.selectById(id);
        if (dynamic == null) {
            throw new IllegalArgumentException("动态不存在");
        }
        if (!dynamic.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人动态");
        }
        dynamic.setStatus(0);
        dynamicMapper.updateById(dynamic);
        return true;
    }

    @Override
    public IPage<DynamicVO> getList(int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        Page<UserDynamic> pageObj = new Page<>(page, size);
        IPage<UserDynamic> dynamicPage = dynamicMapper.selectPage(pageObj,
                new LambdaQueryWrapper<UserDynamic>()
                        .eq(UserDynamic::getStatus, 1)
                        .orderByDesc(UserDynamic::getCreatedAt));
        return convertPage(dynamicPage, myId);
    }

    @Override
    public DynamicVO getDetail(Long id) {
        Long myId = StpUtil.getLoginIdAsLong();
        UserDynamic dynamic = dynamicMapper.selectById(id);
        if (dynamic == null || (dynamic.getStatus() != null && dynamic.getStatus() != 1)) {
            throw new IllegalArgumentException("动态不存在");
        }
        User user = userMapper.selectById(dynamic.getUserId());
        DynamicVO vo = toVO(dynamic, user != null ? user.getNickname() : null, user != null ? user.getAvatar() : null);
        vo.setLiked(checkLikedByUser(id, myId));
        return vo;
    }

    @Override
    @CircuitBreaker(name = "user-dynamic-db")
    @Retry(name = "mysql-retry")
    public IPage<DynamicVO> getMyDynamics(int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        return getUserDynamics(myId, page, size);
    }

    @Override
    public IPage<DynamicVO> getUserDynamics(Long userId, int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        Page<UserDynamic> pageObj = new Page<>(page, size);
        IPage<UserDynamic> dynamicPage = dynamicMapper.selectPage(pageObj,
                new LambdaQueryWrapper<UserDynamic>()
                        .eq(UserDynamic::getUserId, userId)
                        .eq(UserDynamic::getStatus, 1)
                        .orderByDesc(UserDynamic::getCreatedAt));
        return convertPage(dynamicPage, myId);
    }

    private IPage<DynamicVO> convertPage(IPage<UserDynamic> dynamicPage, Long currentUserId) {
        List<UserDynamic> dynamics = dynamicPage.getRecords();
        if (dynamics.isEmpty()) {
            Page<DynamicVO> empty = new Page<>(dynamicPage.getCurrent(), dynamicPage.getSize());
            empty.setRecords(new ArrayList<>());
            empty.setTotal(dynamicPage.getTotal());
            return empty;
        }

        Set<Long> userIds = dynamics.stream().map(UserDynamic::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> dynamicIds = dynamics.stream().map(UserDynamic::getId).collect(Collectors.toSet());
        Set<Long> likedIds = likeMapper.selectList(
                new LambdaQueryWrapper<DynamicLike>()
                        .eq(DynamicLike::getUserId, currentUserId)
                        .eq(DynamicLike::getStatus, 1)
                        .in(DynamicLike::getDynamicId, dynamicIds))
                .stream().map(DynamicLike::getDynamicId).collect(Collectors.toSet());

        return dynamicPage.convert(d -> {
            User u = userMap.get(d.getUserId());
            DynamicVO vo = toVO(d, u != null ? u.getNickname() : null, u != null ? u.getAvatar() : null);
            vo.setLiked(likedIds.contains(d.getId()));
            return vo;
        });
    }

    private Boolean checkLikedByUser(Long dynamicId, Long userId) {
        Long count = likeMapper.selectCount(
                new LambdaQueryWrapper<DynamicLike>()
                        .eq(DynamicLike::getDynamicId, dynamicId)
                        .eq(DynamicLike::getUserId, userId)
                        .eq(DynamicLike::getStatus, 1));
        return count > 0;
    }

    private DynamicVO toVO(UserDynamic dynamic, String nickname, String avatar) {
        DynamicVO vo = new DynamicVO();
        BeanUtils.copyProperties(dynamic, vo);
        vo.setNickname(nickname);
        vo.setAvatar(avatar);
        vo.setLiked(false);
        return vo;
    }
}
