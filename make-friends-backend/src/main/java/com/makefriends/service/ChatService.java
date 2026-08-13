package com.makefriends.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.dto.SendMessageDTO;
import com.makefriends.entity.ChatMessage;
import com.makefriends.vo.ChatSessionVO;

import java.util.List;

public interface ChatService {

    List<ChatSessionVO> getSessions();

    IPage<ChatMessage> getMessages(Long sessionId, int page, int size);

    ChatMessage sendMessage(SendMessageDTO dto);

    boolean markAsRead(Long sessionId);

    Integer getUnreadCount();

    ChatSessionVO getOrCreateSession(Long otherUserId);

    void deleteSession(Long sessionId);
}
