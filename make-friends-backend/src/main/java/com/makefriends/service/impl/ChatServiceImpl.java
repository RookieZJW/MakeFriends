package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.makefriends.dto.SendMessageDTO;
import com.makefriends.entity.ChatMessage;
import com.makefriends.entity.ChatSession;
import com.makefriends.entity.User;
import com.makefriends.mapper.ChatMessageMapper;
import com.makefriends.mapper.ChatSessionMapper;
import com.makefriends.mapper.UserMapper;
import com.makefriends.service.ChatService;
import com.makefriends.vo.ChatSessionVO;
import com.makefriends.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChatWebSocketHandler webSocketHandler;
    private final com.makefriends.service.UserService userService;

    public ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper,
                           UserMapper userMapper, ChatWebSocketHandler webSocketHandler,
                           com.makefriends.service.UserService userService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.webSocketHandler = webSocketHandler;
        this.userService = userService;
    }

    @Override
    @CircuitBreaker(name = "chat-channel-db")
    @Retry(name = "mysql-retry")
    public List<ChatSessionVO> getSessions() {
        Long myId = StpUtil.getLoginIdAsLong();
        // 简单查询：只要包含我且 status=1 的会话
        List<ChatSession> allSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .and(w -> w.eq(ChatSession::getUser1Id, myId).or().eq(ChatSession::getUser2Id, myId))
                        .eq(ChatSession::getStatus, 1)
                        .orderByDesc(ChatSession::getUpdatedAt));

        // 在 Java 内存中过滤掉"我已删除"的会话（兼容 user1Deleted/user2Deleted 列不存在的情况）
        List<ChatSession> sessions = allSessions.stream().filter(s -> {
            if (s.getUser1Id().equals(myId)) {
                return s.getUser1Deleted() == null || s.getUser1Deleted() != 1;
            } else {
                return s.getUser2Deleted() == null || s.getUser2Deleted() != 1;
            }
        }).collect(Collectors.toList());

        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> otherUserIds = sessions.stream()
                .map(s -> s.getUser1Id().equals(myId) ? s.getUser2Id() : s.getUser1Id())
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> lastMsgIds = sessions.stream()
                .map(ChatSession::getLastMsgId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, ChatMessage> msgMap = lastMsgIds.isEmpty() ? Map.of()
                : messageMapper.selectBatchIds(lastMsgIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        List<ChatSessionVO> voList = new ArrayList<>();
        for (ChatSession s : sessions) {
            Long otherId = s.getUser1Id().equals(myId) ? s.getUser2Id() : s.getUser1Id();
            User other = userMap.get(otherId);
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setUserId(otherId);
            vo.setNickname(other != null ? other.getNickname() : null);
            vo.setAvatar(other != null ? other.getAvatar() : null);
            ChatMessage lastMsg = s.getLastMsgId() != null ? msgMap.get(s.getLastMsgId()) : null;
            vo.setLastMessage(lastMsg != null ? lastMsg.getContent() : null);
            vo.setLastMsgTime(lastMsg != null ? lastMsg.getCreatedAt() : s.getUpdatedAt());

            Long unread = messageMapper.selectCount(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSessionId, s.getId())
                            .eq(ChatMessage::getReceiverId, myId)
                            .eq(ChatMessage::getIsRead, 0));
            vo.setUnreadCount(unread.intValue());
            vo.setOnline(userService.isUserOnline(otherId));
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public IPage<ChatMessage> getMessages(Long sessionId, int page, int size) {
        Long myId = StpUtil.getLoginIdAsLong();
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUser1Id().equals(myId) && !session.getUser2Id().equals(myId)) {
            throw new IllegalArgumentException("无权查看该会话");
        }

        Long total = messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId));

        Page<ChatMessage> pageObj = new Page<>(page, size);
        pageObj.setTotal(total);
        pageObj.setCurrent(page);
        pageObj.setSize(size);

        if (total == 0) {
            pageObj.setRecords(new ArrayList<>());
            return pageObj;
        }

        List<ChatMessage> descList = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT " + (page - 1) * size + ", " + size));

        Collections.reverse(descList);
        pageObj.setRecords(descList);
        return pageObj;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CircuitBreaker(name = "chat-channel-db")
    @Retry(name = "mysql-retry")
    public ChatMessage sendMessage(SendMessageDTO dto) {
        Long senderId = StpUtil.getLoginIdAsLong();
        Long receiverId = dto.getReceiverId();

        if (receiverId.equals(senderId)) {
            throw new IllegalArgumentException("不能给自己发送消息");
        }

        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new IllegalArgumentException("接收者不存在");
        }

        ChatSession session = null;
        if (dto.getSessionId() != null) {
            session = sessionMapper.selectById(dto.getSessionId());
        }

        if (session == null) {
            Long minId = Math.min(senderId, receiverId);
            Long maxId = Math.max(senderId, receiverId);
            session = sessionMapper.selectOne(
                    new LambdaQueryWrapper<ChatSession>()
                            .eq(ChatSession::getUser1Id, minId)
                            .eq(ChatSession::getUser2Id, maxId)
                            .eq(ChatSession::getStatus, 1)
                            .last("LIMIT 1"));
            if (session == null) {
                session = new ChatSession();
                session.setUser1Id(minId);
                session.setUser2Id(maxId);
                session.setUnreadCount(0);
                session.setStatus(1);
                session.setUser1Deleted(0);
                session.setUser2Deleted(0);
                sessionMapper.insert(session);
            }
        }
        // 发送消息时，如果我之前删除了该会话，则恢复（重新显示在列表中）
        boolean needUpdate = false;
        if (session.getUser1Id().equals(senderId) && session.getUser1Deleted() != null && session.getUser1Deleted() == 1) {
            session.setUser1Deleted(0);
            needUpdate = true;
        } else if (session.getUser2Id().equals(senderId) && session.getUser2Deleted() != null && session.getUser2Deleted() == 1) {
            session.setUser2Deleted(0);
            needUpdate = true;
        }
        if (needUpdate) {
            sessionMapper.updateById(session);
        }

        if (!session.getUser1Id().equals(senderId) && !session.getUser2Id().equals(senderId)) {
            throw new IllegalArgumentException("无权在该会话中发送消息");
        }
        if (!receiverId.equals(session.getUser1Id()) && !receiverId.equals(session.getUser2Id())) {
            throw new IllegalArgumentException("接收者不在该会话中");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMsgType(dto.getMsgType());
        message.setContent(dto.getContent());
        message.setIsRead(0);
        messageMapper.insert(message);

        session.setLastMsgId(message.getId());
        Long receiverUnread = messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, session.getId())
                        .eq(ChatMessage::getReceiverId, receiverId)
                        .eq(ChatMessage::getIsRead, 0));
        session.setUnreadCount(receiverUnread.intValue());
        sessionMapper.updateById(session);

        try {
            JSONObject wsMsg = new JSONObject();
            wsMsg.set("type", "chat");
            wsMsg.set("sessionId", session.getId());
            wsMsg.set("senderId", senderId);
            wsMsg.set("receiverId", receiverId);
            wsMsg.set("msgType", dto.getMsgType());
            wsMsg.set("content", dto.getContent());
            wsMsg.set("msgId", message.getId());
            wsMsg.set("createdAt", message.getCreatedAt());
            webSocketHandler.sendMessageToUser(receiverId, wsMsg.toString());
        } catch (Exception e) {
        }

        return message;
    }

    @Override
    @Transactional
    public boolean markAsRead(Long sessionId) {
        Long myId = StpUtil.getLoginIdAsLong();
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUser1Id().equals(myId) && !session.getUser2Id().equals(myId)) {
            throw new IllegalArgumentException("无权操作该会话");
        }

        messageMapper.update(null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getReceiverId, myId)
                        .eq(ChatMessage::getIsRead, 0)
                        .set(ChatMessage::getIsRead, 1));

        session.setUnreadCount(0);
        sessionMapper.updateById(session);
        return true;
    }

    @Override
    public Integer getUnreadCount() {
        Long myId = StpUtil.getLoginIdAsLong();
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getReceiverId, myId)
                        .eq(ChatMessage::getIsRead, 0));
        return count.intValue();
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Long myId = StpUtil.getLoginIdAsLong();
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getStatus().equals(1)) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUser1Id().equals(myId) && !session.getUser2Id().equals(myId)) {
            throw new IllegalArgumentException("无权操作该会话");
        }

        boolean isUser1 = session.getUser1Id().equals(myId);

        // 1. 标记我这边删除
        if (isUser1) {
            session.setUser1Deleted(1);
        } else {
            session.setUser2Deleted(1);
        }
        sessionMapper.updateById(session);

        // 2. 检查是否双方都已删除：若是，物理删除会话和所有消息
        boolean u1Del = session.getUser1Deleted() != null && session.getUser1Deleted() == 1;
        boolean u2Del = session.getUser2Deleted() != null && session.getUser2Deleted() == 1;
        if (u1Del && u2Del) {
            // 物理删除该会话的所有消息
            messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId));
            // 物理删除会话
            sessionMapper.deleteById(sessionId);
        }
    }

    @Override
    public ChatSessionVO getOrCreateSession(Long otherUserId) {
        Long myId = StpUtil.getLoginIdAsLong();
        if (otherUserId.equals(myId)) {
            throw new IllegalArgumentException("不能和自己创建会话");
        }
        User other = userMapper.selectById(otherUserId);
        if (other == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Long minId = Math.min(myId, otherUserId);
        Long maxId = Math.max(myId, otherUserId);

        ChatSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUser1Id, minId)
                        .eq(ChatSession::getUser2Id, maxId)
                        .eq(ChatSession::getStatus, 1)
                        .last("LIMIT 1"));

        if (session == null) {
            session = new ChatSession();
            session.setUser1Id(minId);
            session.setUser2Id(maxId);
            session.setUnreadCount(0);
            session.setStatus(1);
            session.setUser1Deleted(0);
            session.setUser2Deleted(0);
            sessionMapper.insert(session);
        } else {
            // 如果我之前删除过，恢复
            boolean needUpdate = false;
            if (session.getUser1Id().equals(myId) && session.getUser1Deleted() != null && session.getUser1Deleted() == 1) {
                session.setUser1Deleted(0);
                needUpdate = true;
            } else if (session.getUser2Id().equals(myId) && session.getUser2Deleted() != null && session.getUser2Deleted() == 1) {
                session.setUser2Deleted(0);
                needUpdate = true;
            }
            if (needUpdate) {
                sessionMapper.updateById(session);
            }
        }

        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setUserId(otherUserId);
        vo.setNickname(other.getNickname());
        vo.setAvatar(other.getAvatar());
        vo.setUnreadCount(0);
        vo.setOnline(userService.isUserOnline(otherUserId));
        return vo;
    }
}
