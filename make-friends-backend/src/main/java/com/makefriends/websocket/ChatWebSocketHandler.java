package com.makefriends.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            USER_SESSIONS.put(userId, session);
            log.info("用户 {} WebSocket 连接建立, 当前在线: {}", userId, USER_SESSIONS.size());
        } else {
            log.warn("WebSocket 连接未携带 userId, 关闭连接");
            closeSession(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long senderId = getUserIdFromSession(session);
        if (senderId == null) {
            return;
        }
        try {
            JSONObject payload = JSONUtil.parseObj(message.getPayload());
            Long receiverId = payload.getLong("receiverId");
            if (receiverId == null) {
                return;
            }
            payload.set("senderId", senderId);
            String msg = payload.toString();
            sendMessageToUser(receiverId, msg);
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: ", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            USER_SESSIONS.remove(userId);
            log.info("用户 {} WebSocket 连接关闭, 当前在线: {}", userId, USER_SESSIONS.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserIdFromSession(session);
        log.error("用户 {} WebSocket 传输错误: {}", userId, exception.getMessage());
        if (session.isOpen()) {
            closeSession(session);
        }
        if (userId != null) {
            USER_SESSIONS.remove(userId);
        }
    }

    public void sendMessageToUser(Long userId, String message) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息给用户 {} 失败: {}", userId, e.getMessage());
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if ("userId".equals(pair[0]) && pair.length == 2) {
                try {
                    return Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private void closeSession(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (IOException e) {
            log.error("关闭 WebSocket 会话失败: ", e);
        }
    }
}
