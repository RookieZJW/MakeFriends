package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.makefriends.entity.ChatSession;
import com.makefriends.entity.User;
import com.makefriends.entity.UserMatch;
import com.makefriends.mapper.ChatSessionMapper;
import com.makefriends.mapper.UserMapper;
import com.makefriends.mapper.UserMatchMapper;
import com.makefriends.service.UserMatchService;
import com.makefriends.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserMatchServiceImpl implements UserMatchService {

    private final UserMatchMapper userMatchMapper;
    private final UserMapper userMapper;
    private final ChatSessionMapper chatSessionMapper;

    public UserMatchServiceImpl(UserMatchMapper userMatchMapper, UserMapper userMapper, ChatSessionMapper chatSessionMapper) {
        this.userMatchMapper = userMatchMapper;
        this.userMapper = userMapper;
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    @Transactional
    public boolean likeUser(Long toUserId) {
        try {
            return doLikeUser(toUserId);
        } catch (DuplicateKeyException duplicateKey) {
            // 幂等兜底：并发两次点击同时通过了 selectOne，触发 uk_from_to 唯一键冲突
            // 此时另一条记录一定已经插入成功，再跑一次 doLikeUser -> existing 查询命中，返回正常结果
            return doLikeUser(toUserId);
        }
    }

    private boolean doLikeUser(Long toUserId) {
        Long fromUserId = StpUtil.getLoginIdAsLong();
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("不能喜欢自己");
        }
        User target = userMapper.selectById(toUserId);
        if (target == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }

        // 修复：不按 status 过滤，只要 (from,to) 存在就算已有记录，匹配 uk_from_to 唯一键语义
        UserMatch existing = userMatchMapper.selectOne(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, fromUserId)
                        .eq(UserMatch::getToUserId, toUserId)
                        .last("LIMIT 1"));

        boolean alreadyLiked = existing != null
                && existing.getStatus() != null && existing.getStatus() == 1
                && existing.getMatchType() != null && (existing.getMatchType() == 1 || existing.getMatchType() == 3);
        if (alreadyLiked) {
            // 幂等：重复心动不再抛异常，直接返回当前是否已是双向匹配
            return existing.getMatchType() == 3;
        }

        UserMatch reverse = userMatchMapper.selectOne(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, toUserId)
                        .eq(UserMatch::getToUserId, fromUserId)
                        .last("LIMIT 1"));
        boolean reverseLiked = reverse != null
                && reverse.getStatus() != null && reverse.getStatus() == 1
                && reverse.getMatchType() != null && (reverse.getMatchType() == 1 || reverse.getMatchType() == 3);

        if (reverseLiked) {
            // 对方也喜欢我 -> 升级为双向匹配
            if (existing != null) {
                existing.setMatchType(3);
                existing.setStatus(1);
                userMatchMapper.updateById(existing);
            } else {
                UserMatch match = new UserMatch();
                match.setFromUserId(fromUserId);
                match.setToUserId(toUserId);
                match.setMatchType(3);
                match.setStatus(1);
                userMatchMapper.insert(match);
            }
            if (reverse.getMatchType() == null || reverse.getMatchType() != 3) {
                reverse.setMatchType(3);
                reverse.setStatus(1);
                userMatchMapper.updateById(reverse);
            }
            createSessionIfNotExists(fromUserId, toUserId);
            return true;
        } else {
            // 单向心动：有历史记录就 update（upsert），避免与 uk_from_to 冲突
            if (existing != null) {
                existing.setMatchType(1);
                existing.setStatus(1);
                userMatchMapper.updateById(existing);
            } else {
                UserMatch match = new UserMatch();
                match.setFromUserId(fromUserId);
                match.setToUserId(toUserId);
                match.setMatchType(1);
                match.setStatus(1);
                userMatchMapper.insert(match);
            }
            return false;
        }
    }

    @Override
    @Transactional
    public boolean unlikeUser(Long toUserId) {
        Long fromUserId = StpUtil.getLoginIdAsLong();
        // 同样不按 status 过滤，只要存在记录就处理
        UserMatch existing = userMatchMapper.selectOne(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, fromUserId)
                        .eq(UserMatch::getToUserId, toUserId)
                        .last("LIMIT 1"));

        // 幂等：没记录 / 已经取消过，直接返回成功，不抛错
        if (existing == null) {
            return true;
        }
        if (existing.getStatus() != null && existing.getStatus() == 0
                && existing.getMatchType() != null && existing.getMatchType() == 2) {
            return true;
        }

        boolean wasMutual = existing.getMatchType() != null && existing.getMatchType() == 3;
        existing.setMatchType(2);
        existing.setStatus(0);
        userMatchMapper.updateById(existing);

        if (wasMutual) {
            UserMatch reverse = userMatchMapper.selectOne(
                    new LambdaQueryWrapper<UserMatch>()
                            .eq(UserMatch::getFromUserId, toUserId)
                            .eq(UserMatch::getToUserId, fromUserId)
                            .last("LIMIT 1"));
            if (reverse != null && reverse.getStatus() != null && reverse.getStatus() == 1) {
                // 反向从"互相匹配"降回"对方单向喜欢我"
                reverse.setMatchType(1);
                userMatchMapper.updateById(reverse);
            }
        }
        return true;
    }

    @Override
    public Integer getMatchStatus(Long userId) {
        Long myId = StpUtil.getLoginIdAsLong();
        UserMatch iLike = userMatchMapper.selectOne(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, myId)
                        .eq(UserMatch::getToUserId, userId)
                        .eq(UserMatch::getStatus, 1)
                        .last("LIMIT 1"));
        UserMatch likesMe = userMatchMapper.selectOne(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, userId)
                        .eq(UserMatch::getToUserId, myId)
                        .eq(UserMatch::getStatus, 1)
                        .last("LIMIT 1"));

        boolean iLikeHim = iLike != null && (iLike.getMatchType() == 1 || iLike.getMatchType() == 3);
        boolean heLikesMe = likesMe != null && (likesMe.getMatchType() == 1 || likesMe.getMatchType() == 3);

        if (iLikeHim && heLikesMe) {
            return 3;
        } else if (iLikeHim) {
            return 1;
        } else if (heLikesMe) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public IPage<UserVO> getMyLikes(int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        List<UserMatch> matches = userMatchMapper.selectList(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, myId)
                        .eq(UserMatch::getStatus, 1)
                        .in(UserMatch::getMatchType, 1, 3)
                        .orderByDesc(UserMatch::getCreatedAt));
        return buildUserPage(matches, page, size, true);
    }

    @Override
    public IPage<UserVO> getWhoLikesMe(int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        List<UserMatch> matches = userMatchMapper.selectList(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getToUserId, myId)
                        .eq(UserMatch::getStatus, 1)
                        .in(UserMatch::getMatchType, 1, 3)
                        .orderByDesc(UserMatch::getCreatedAt));
        return buildUserPage(matches, page, size, false);
    }

    @Override
    public IPage<UserVO> getMutualMatches(int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();

        // 1. 查我喜欢的人（matchType=1 或 3，status=1）
        List<UserMatch> myLikes = userMatchMapper.selectList(
                new LambdaQueryWrapper<UserMatch>()
                        .eq(UserMatch::getFromUserId, myId)
                        .eq(UserMatch::getStatus, 1)
                        .in(UserMatch::getMatchType, 1, 3)
                        .orderByDesc(UserMatch::getCreatedAt));

        if (myLikes.isEmpty()) {
            Page<UserVO> empty = new Page<>(page, size);
            empty.setTotal(0);
            empty.setRecords(new ArrayList<>());
            return empty;
        }

        // 2. 查喜欢我的人的 ID 集合
        Set<Long> likedMeIds = userMatchMapper.selectList(
                        new LambdaQueryWrapper<UserMatch>()
                                .eq(UserMatch::getToUserId, myId)
                                .eq(UserMatch::getStatus, 1)
                                .in(UserMatch::getMatchType, 1, 3))
                .stream()
                .map(UserMatch::getFromUserId)
                .collect(Collectors.toSet());

        // 3. 取交集 = 互相匹配
        List<UserMatch> mutual = myLikes.stream()
                .filter(m -> likedMeIds.contains(m.getToUserId()))
                .collect(Collectors.toList());

        // 4. 修复历史数据：如果 matchType 还是 1，升级为 3
        for (UserMatch m : mutual) {
            if (m.getMatchType() == null || m.getMatchType() != 3) {
                m.setMatchType(3);
                userMatchMapper.updateById(m);
                UserMatch reverse = userMatchMapper.selectOne(
                        new LambdaQueryWrapper<UserMatch>()
                                .eq(UserMatch::getFromUserId, m.getToUserId())
                                .eq(UserMatch::getToUserId, myId)
                                .eq(UserMatch::getStatus, 1)
                                .last("LIMIT 1"));
                if (reverse != null && (reverse.getMatchType() == null || reverse.getMatchType() != 3)) {
                    reverse.setMatchType(3);
                    userMatchMapper.updateById(reverse);
                }
                createSessionIfNotExists(myId, m.getToUserId());
            }
        }

        return buildUserPage(mutual, page, size, true);
    }

    @Override
    public Map<String, Long> getMatchCounts() {
        Long myId = StpUtil.getLoginIdAsLong();
        Map<String, Long> counts = new HashMap<>();
        counts.put("match", 0L);
        counts.put("myLike", 0L);
        counts.put("likedMe", 0L);
        try {
            // 互相匹配：我喜欢的 (matchType=1 或 3, status=1) 且对方也喜欢我
            List<UserMatch> myLikes = userMatchMapper.selectList(
                    new LambdaQueryWrapper<UserMatch>()
                            .eq(UserMatch::getFromUserId, myId)
                            .eq(UserMatch::getStatus, 1)
                            .in(UserMatch::getMatchType, 1, 3));
            Set<Long> likedMeIds = userMatchMapper.selectList(
                            new LambdaQueryWrapper<UserMatch>()
                                    .eq(UserMatch::getToUserId, myId)
                                    .eq(UserMatch::getStatus, 1)
                                    .in(UserMatch::getMatchType, 1, 3))
                    .stream()
                    .map(UserMatch::getFromUserId)
                    .collect(Collectors.toSet());
            long matchCount = myLikes.stream()
                    .filter(m -> likedMeIds.contains(m.getToUserId()))
                    .count();
            counts.put("match", matchCount);
            counts.put("myLike", (long) myLikes.size());
            counts.put("likedMe", (long) likedMeIds.size());
        } catch (Exception e) {
            // 兜底：查询失败返回 0，不让计数接口炸 500
        }
        return counts;
    }

    private void createSessionIfNotExists(Long user1, Long user2) {
        Long minId = Math.min(user1, user2);
        Long maxId = Math.max(user1, user2);
        ChatSession existing = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUser1Id, minId)
                        .eq(ChatSession::getUser2Id, maxId)
                        .eq(ChatSession::getStatus, 1)
                        .last("LIMIT 1"));
        if (existing == null) {
            ChatSession session = new ChatSession();
            session.setUser1Id(minId);
            session.setUser2Id(maxId);
            session.setUnreadCount(0);
            session.setStatus(1);
            chatSessionMapper.insert(session);
        }
    }

    private IPage<UserVO> buildUserPage(List<UserMatch> matches, int page, int size, boolean useToUserId) {
        Page<UserVO> pageObj = new Page<>(page, size);
        pageObj.setTotal(matches.size());
        pageObj.setCurrent(page);
        pageObj.setSize(size);

        if (matches.isEmpty()) {
            pageObj.setRecords(new ArrayList<>());
            return pageObj;
        }

        int fromIdx = (page - 1) * size;
        int toIdx = Math.min(fromIdx + size, matches.size());
        if (fromIdx >= matches.size()) {
            pageObj.setRecords(new ArrayList<>());
            return pageObj;
        }

        List<UserMatch> pageMatches = matches.subList(fromIdx, toIdx);
        List<Long> userIds = pageMatches.stream()
                .map(m -> useToUserId ? m.getToUserId() : m.getFromUserId())
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UserVO> voList = new ArrayList<>();
        for (Long uid : userIds) {
            User u = userMap.get(uid);
            if (u != null) {
                UserVO vo = new UserVO();
                BeanUtils.copyProperties(u, vo);
                voList.add(vo);
            }
        }
        pageObj.setRecords(voList);
        return pageObj;
    }
}
