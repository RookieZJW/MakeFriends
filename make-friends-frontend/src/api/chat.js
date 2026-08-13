import request from './request'

// 会话列表
export function getChatList() {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}

// 聊天记录（分页）
export function getMessages(sessionId, params) {
  return request({
    url: `/chat/messages/${sessionId}`,
    method: 'get',
    params
  })
}

// 发送消息（不传 sessionId 时后端自动创建会话）
export function sendMessage(data) {
  return request({
    url: '/chat/send',
    method: 'post',
    data
  })
}

// 标记已读
export function markRead(sessionId) {
  return request({
    url: `/chat/read/${sessionId}`,
    method: 'put'
  })
}

// 未读消息总数
export function getUnreadCount() {
  return request({
    url: '/chat/unread-count',
    method: 'get'
  })
}

// 创建或获取与某用户的会话（互相匹配后点聊天用）
export function createOrGetSession(userId) {
  return request({
    url: `/chat/session/${userId}`,
    method: 'post'
  })
}

// 删除会话（软删除：仅当前用户端删除；双方都删则物理删除会话和消息）
export function deleteSession(sessionId) {
  return request({
    url: `/chat/session/${sessionId}`,
    method: 'delete'
  })
}
