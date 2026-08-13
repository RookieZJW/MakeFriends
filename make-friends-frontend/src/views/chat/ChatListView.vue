<template>
  <div class="chat-list-view">
    <div class="mf-container">
      <div class="chat-head">
        <h1 class="page-title"><span class="bar"></span>我的消息</h1>
        <p class="page-sub">和心动的人聊聊天吧 ♡</p>
      </div>

      <div class="chat-list mf-card" v-loading="loading">
        <div
          class="chat-item"
          v-for="s in sessions"
          :key="s.id"
          @click="enterChat(s)"
        >
          <div class="avatar-wrap">
            <el-avatar :size="52" :src="resolveAvatar(s.avatar, s.nickname)" />
            <span class="online-dot" v-if="s.online"></span>
          </div>
          <div class="chat-meta">
            <div class="meta-top">
              <span class="nickname">{{ s.nickname || '未知用户' }}</span>
              <span class="time">{{ formatRelativeTime(s.lastTime || s.updatedAt) }}</span>
            </div>
            <div class="meta-bottom">
              <span class="last-msg">{{ s.lastMessage || '暂无消息' }}</span>
              <span class="unread" v-if="s.unreadCount > 0">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
            </div>
          </div>
          <button
            class="del-btn"
            @click.stop="onDelete($event, s)"
            title="删除会话"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>

        <div v-if="!loading && sessions.length === 0" class="empty-state">
          <div class="empty-emoji">💬</div>
          <p>还没有任何会话，去喜欢一个人开启聊天吧~</p>
          <button class="mf-btn" @click="$router.push('/home')">去发现</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getChatList, deleteSession } from '@/api/chat'
import { resolveAvatar, formatRelativeTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const sessions = ref([])

async function loadList() {
  loading.value = true
  try {
    const res = await getChatList()
    const list = res.data || res || []
    // 规范字段：后端 ChatSessionVO 返回 { id, userId, nickname, avatar, lastMessage, unreadCount, lastMsgTime }
    sessions.value = (Array.isArray(list) ? list : []).map(s => ({
      ...s,
      nickname: s.nickname || s.peerNickname || '未知用户',
      avatar: s.avatar || s.peerAvatar || '',
      lastMessage: s.lastMessage || s.lastMsg || '',
      lastTime: s.lastMsgTime || s.lastTime || s.updatedAt,
      unreadCount: s.unreadCount || 0
    }))
  } catch (e) {
    sessions.value = []
  } finally {
    loading.value = false
  }
}

function enterChat(s) {
  router.push(`/chat/${s.id}`)
}

async function onDelete(e, s) {
  e.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定要删除与「${s.nickname || '该用户'}」的会话吗？\n删除后将从列表中移除，对方仍能看到聊天记录。`,
      '删除会话',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (err) {
    return
  }
  try {
    await deleteSession(s.id)
    ElMessage.success('会话已删除')
    await loadList()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  }
}

let refreshTimer = null
onMounted(() => {
  loadList()
  // 15 秒刷新一次列表，更新对方在线状态 + 未读
  refreshTimer = setInterval(loadList, 15 * 1000)
})
onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = null
})
</script>

<style lang="scss" scoped>
.chat-list-view {
  padding: 30px 0 40px;
}

.chat-head {
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 26px;
  font-weight: 800;
  color: #2d2d3a;
  margin: 0 0 6px;

  .bar {
    width: 6px;
    height: 26px;
    border-radius: 3px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
  }
}

.page-sub {
  margin: 0;
  font-size: 14px;
  color: #9a9aaa;
  padding-left: 16px;
}

.chat-list {
  max-width: 720px;
  margin: 0 auto;
  padding: 8px 0;
  overflow: hidden;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-bottom: 1px solid #f7f7fa;
  position: relative;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
    .del-btn {
      opacity: 1;
    }
  }

  .del-btn {
    opacity: 0;
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    border: none;
    background: rgba(244, 63, 94, 0.08);
    color: #f43f5e;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    &:hover {
      background: #f43f5e;
      color: #fff;
      transform: scale(1.05);
    }
  }
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;

  .online-dot {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #22c55e;
    border: 2px solid #fff;
  }
}

.chat-meta {
  flex: 1;
  min-width: 0;
}

.meta-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;

  .nickname {
    font-size: 16px;
    font-weight: 600;
    color: #2d2d3a;
  }
  .time {
    font-size: 12px;
    color: #aaa;
  }
}

.meta-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .last-msg {
    font-size: 13px;
    color: #9a9aaa;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 360px;
  }

  .unread {
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    border-radius: 999px;
    background: #f43f5e;
    color: #fff;
    font-size: 12px;
    line-height: 20px;
    text-align: center;
    font-weight: 700;
  }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #aaa;

  .empty-emoji {
    font-size: 60px;
    margin-bottom: 12px;
  }
  .mf-btn {
    margin-top: 16px;
  }
}

/* ================== ChatListView 响应式 ================== */
@media (max-width: 1023px) {
  .chat-list-view { padding: 20px 0 30px; }
  .page-title { font-size: 22px; .bar { height: 22px; width: 5px; } }
  .chat-list { max-width: 100%; }
}
@media (max-width: 767px) {
  .chat-list-view { padding: 14px 0 24px; }
  .page-title { font-size: 19px; gap: 8px; margin-bottom: 4px; .bar { height: 18px; width: 4px; } }
  .page-sub { font-size: 13px; padding-left: 12px; }
  .chat-list { padding: 4px 0; }
  .chat-item { padding: 12px 14px; gap: 10px; }
  .chat-item .chat-info .chat-name { font-size: 14px; }
  .chat-item .chat-info .chat-last { font-size: 12.5px; }
  .chat-meta {
    flex-direction: column-reverse; align-items: flex-end; gap: 4px; min-width: 60px;
    .chat-time { font-size: 10.5px; }
  }
  .empty-state { padding: 48px 20px; .empty-emoji { font-size: 44px; } .mf-btn { width: 100%; } }
}
@media (max-width: 479px) {
  .chat-head { margin-bottom: 14px; }
  .page-title { font-size: 17px; }
  .page-sub { font-size: 12px; padding-left: 9px; }
  .chat-item { padding: 10px 10px; gap: 8px; }
  .chat-item .del-btn { width: 28px; height: 28px; }
  .avatar-wrap :deep(.el-avatar) { --el-avatar-size: 40px !important; }
}
</style>
