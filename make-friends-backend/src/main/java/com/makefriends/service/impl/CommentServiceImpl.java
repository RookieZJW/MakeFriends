package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.makefriends.dto.CommentDTO;
import com.makefriends.entity.DynamicComment;
import com.makefriends.entity.User;
import com.makefriends.entity.UserDynamic;
import com.makefriends.mapper.DynamicCommentMapper;
import com.makefriends.mapper.UserDynamicMapper;
import com.makefriends.mapper.UserMapper;
import com.makefriends.service.CommentService;
import com.makefriends.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final DynamicCommentMapper commentMapper;
    private final UserDynamicMapper dynamicMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(DynamicCommentMapper commentMapper, UserDynamicMapper dynamicMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.dynamicMapper = dynamicMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public DynamicComment addComment(CommentDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDynamic dynamic = dynamicMapper.selectById(dto.getDynamicId());
        if (dynamic == null || (dynamic.getStatus() != null && dynamic.getStatus() != 1)) {
            throw new IllegalArgumentException("动态不存在");
        }
        DynamicComment comment = new DynamicComment();
        comment.setDynamicId(dto.getDynamicId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setStatus(1);
        commentMapper.insert(comment);

        dynamic.setCommentCount((dynamic.getCommentCount() == null ? 0 : dynamic.getCommentCount()) + 1);
        dynamicMapper.updateById(dynamic);
        return comment;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        DynamicComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除他人评论");
        }
        if (comment.getStatus() != null && comment.getStatus() == 0) {
            return true;
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);

        UserDynamic dynamic = dynamicMapper.selectById(comment.getDynamicId());
        if (dynamic != null) {
            int count = (dynamic.getCommentCount() == null ? 0 : dynamic.getCommentCount()) - 1;
            dynamic.setCommentCount(Math.max(count, 0));
            dynamicMapper.updateById(dynamic);
        }
        return true;
    }

    @Override
    public List<CommentVO> getCommentsByDynamicId(Long dynamicId) {
        List<DynamicComment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<DynamicComment>()
                        .eq(DynamicComment::getDynamicId, dynamicId)
                        .eq(DynamicComment::getStatus, 1)
                        .orderByAsc(DynamicComment::getCreatedAt));

        List<Long> userIds = comments.stream()
                .map(DynamicComment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setDynamicId(comment.getDynamicId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setParentId(comment.getParentId());
            vo.setCreatedAt(comment.getCreatedAt());

            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            if (comment.getParentId() != null) {
                DynamicComment parentComment = commentMapper.selectById(comment.getParentId());
                if (parentComment != null) {
                    User parentUser = userMap.get(parentComment.getUserId());
                    if (parentUser != null) {
                        vo.setReplyToUserId(parentUser.getId());
                        vo.setReplyToNickname(parentUser.getNickname());
                    }
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CommentVO addCommentAndReturnVO(CommentDTO dto) {
        DynamicComment comment = addComment(dto);

        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setDynamicId(comment.getDynamicId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setCreatedAt(comment.getCreatedAt());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        if (comment.getParentId() != null) {
            DynamicComment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment != null) {
                User parentUser = userMapper.selectById(parentComment.getUserId());
                if (parentUser != null) {
                    vo.setReplyToUserId(parentUser.getId());
                    vo.setReplyToNickname(parentUser.getNickname());
                }
            }
        }

        return vo;
    }
}