<template>
  <div class="comment-item" :class="{ 'is-child': isChild }">
    <el-avatar :size="isChild ? 32 : 38" :src="avatarUrl" class="avatar" @click="goUser" />
    <div class="comment-body">
      <div class="comment-main">
        <span class="nickname" @click="goUser">{{ data.nickname || '匿名用户' }}</span>
        <template v-if="data.replyToNickname">
          <span class="reply-arrow">回复</span>
          <span class="reply-to-name" @click="goReplyUser">@{{ data.replyToNickname }}</span>
        </template>
        <span class="content">{{ data.content }}</span>
      </div>
      <div class="comment-foot">
        <span class="time">{{ formatRelativeTime(data.createdAt) }}</span>
        <span class="reply-btn" @click="$emit('reply', data)">回复</span>
      </div>
      <!-- 子回复（仅一级评论展示，最多两层） -->
      <div class="sub-comments" v-if="!isChild && data.children && data.children.length">
        <CommentItem
          v-for="child in data.children"
          :key="child.id"
          :data="child"
          :is-child="true"
          @reply="$emit('reply', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatRelativeTime, resolveAvatar } from '@/utils/format'

const props = defineProps({
  data: { type: Object, required: true },
  isChild: { type: Boolean, default: false }
})

const emit = defineEmits(['reply'])

const router = useRouter()
const avatarUrl = computed(() => resolveAvatar(props.data.avatar, props.data.nickname))

function goUser() {
  if (props.data.userId) router.push(`/user/${props.data.userId}`)
}

function goReplyUser() {
  if (props.data.replyToUserId) router.push(`/user/${props.data.replyToUserId}`)
}
</script>

<style lang="scss" scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;

  &.is-child {
    padding: 10px 0;
    gap: 10px;
  }

  .avatar {
    cursor: pointer;
    flex-shrink: 0;
    border: 2px solid #fff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    transition: transform 0.2s;
    &:hover { transform: scale(1.08); }
  }
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-main {
  font-size: 14px;
  line-height: 1.7;
  color: #3a3a4a;
  word-break: break-word;

  .nickname {
    font-weight: 600;
    color: #ff4f8b;
    cursor: pointer;
    margin-right: 6px;
    &:hover { text-decoration: underline; }
  }

  .reply-arrow {
    color: #aaa;
    font-size: 13px;
    margin: 0 3px;
  }

  .reply-to-name {
    color: #a855f7;
    font-weight: 600;
    cursor: pointer;
    margin-right: 4px;
    &:hover {
      color: #9333ea;
      text-decoration: underline;
    }
  }

  .content {
    color: #3a3a4a;
  }
}

.comment-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 5px;
  font-size: 12px;
  color: #aaa;

  .reply-btn {
    cursor: pointer;
    color: #a855f7;
    font-weight: 500;
    transition: color 0.2s;
    &:hover { color: #9333ea; }
  }
}

.sub-comments {
  margin-top: 8px;
  padding: 4px 0 4px 14px;
  border-left: 2px solid #f3e8ff;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
</style>
