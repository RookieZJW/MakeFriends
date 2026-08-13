package com.makefriends.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.common.Result;
import com.makefriends.dto.SendMessageDTO;
import com.makefriends.entity.ChatMessage;
import com.makefriends.service.ChatService;
import com.makefriends.vo.ChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getSessions() {
        return Result.ok(chatService.getSessions());
    }

    @GetMapping("/messages/{sessionId}")
    public Result<IPage<ChatMessage>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(chatService.getMessages(sessionId, page, size));
    }

    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@Valid @RequestBody SendMessageDTO dto) {
        return Result.ok(chatService.sendMessage(dto));
    }

    @PutMapping("/read/{sessionId}")
    public Result<Void> markAsRead(@PathVariable Long sessionId) {
        chatService.markAsRead(sessionId);
        return Result.ok();
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        Integer count = chatService.getUnreadCount();
        Map<String, Integer> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.ok(result);
    }

    @PostMapping("/session/{userId}")
    public Result<ChatSessionVO> getOrCreateSession(@PathVariable Long userId) {
        return Result.ok(chatService.getOrCreateSession(userId));
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return Result.ok();
    }
}
