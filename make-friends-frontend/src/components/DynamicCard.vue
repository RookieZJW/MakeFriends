<template>
  <div class="dynamic-card mf-card">
    <div class="card-header">
      <el-avatar :size="46" :src="avatarUrl" @click="goUser" class="clickable" />
      <div class="user-meta">
        <span class="username" @click="goUser">{{ data.nickname || data.userName || '匿名用户' }}</span>
        <span class="time">{{ formatRelativeTime(data.createdAt) }}</span>
      </div>
      <el-dropdown v-if="isOwner" trigger="click" @command="onCommand">
        <span class="more-btn"><el-icon><MoreFilled /></el-icon></span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="delete" style="color: #f43f5e">删除动态</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="content" @click="goDetail">{{ data.content }}</div>

    <div class="image-grid" v-if="imageList.length" :class="`grid-${imageList.length}`">
      <div
        v-for="(img, i) in imageList"
        :key="i"
        class="img-item"
        @click="preview(i)"
      >
        <img :src="resolveImage(img)" alt="" loading="lazy" />
      </div>
    </div>

    <div class="card-footer">
      <button class="action-btn" :class="{ active: liked }" @click="onLike">
        <span class="icon" :class="{ 'heart-beat': liked }">{{ liked ? '♥' : '♡' }}</span>
        <span>{{ likeCount }}</span>
      </button>
      <button class="action-btn" @click="goDetail">
        <el-icon><ChatDotRound /></el-icon>
        <span>{{ data.commentCount || 0 }}</span>
      </button>
      <button class="action-btn" @click="onShare">
        <el-icon><Share /></el-icon>
        <span>分享</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MoreFilled, ChatDotRound, Share } from '@element-plus/icons-vue'
import { formatRelativeTime, resolveAvatar, resolveImage } from '@/utils/format'
import { toggleLike, deleteDynamic } from '@/api/dynamic'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['deleted', 'liked'])
const router = useRouter()
const userStore = useUserStore()

const liked = ref(!!props.data.liked)
const likeCount = ref(props.data.likeCount || 0)

const avatarUrl = computed(() => resolveAvatar(props.data.avatar, props.data.nickname))

const imageList = computed(() => {
  const imgs = props.data.images || props.data.imageList
  if (!imgs) return []
  if (Array.isArray(imgs)) return imgs
  return String(imgs).split(',').filter(Boolean)
})

const isOwner = computed(() => {
  return userStore.userId && props.data.userId && Number(userStore.userId) === Number(props.data.userId)
})

function goDetail() {
  router.push(`/dynamic/${props.data.id}`)
}

function goUser() {
  if (props.data.userId) router.push(`/user/${props.data.userId}`)
}

function preview(index) {
  // 简单的图片预览：跳转到详情页
  goDetail()
}

async function onLike() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    await toggleLike(props.data.id)
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
    emit('liked', { id: props.data.id, liked: liked.value })
  } catch (e) {
    // 忽略
  }
}

function onShare() {
  ElMessage.success('链接已复制（演示）')
}

async function onCommand(cmd) {
  if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除这条动态吗？', '提示', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
      await deleteDynamic(props.data.id)
      ElMessage.success('删除成功')
      emit('deleted', props.data.id)
    } catch (e) {
      // 取消或失败
    }
  }
}
</script>

<style lang="scss" scoped>
.dynamic-card {
  padding: 20px 22px;
  margin-bottom: 18px;
  transition: all 0.3s ease;
  &:hover {
    box-shadow: 0 12px 28px rgba(168, 85, 247, 0.14);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;

  .clickable {
    cursor: pointer;
    border: 2px solid #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .user-meta {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 3px;
    .username {
      font-size: 15px;
      font-weight: 600;
      color: #2d2d3a;
      cursor: pointer;
      &:hover {
        color: #ff4f8b;
      }
    }
    .time {
      font-size: 12px;
      color: #aaa;
    }
  }

  .more-btn {
    cursor: pointer;
    padding: 6px;
    border-radius: 50%;
    color: #aaa;
    transition: all 0.25s;
    &:hover {
      background: #f5f5f7;
      color: #666;
    }
  }
}

.content {
  font-size: 15px;
  line-height: 1.7;
  color: #3a3a4a;
  white-space: pre-wrap;
  word-break: break-word;
  cursor: pointer;
  margin-bottom: 14px;
}

.image-grid {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
  border-radius: 10px;
  overflow: hidden;

  &.grid-1 {
    grid-template-columns: 1fr;
    max-width: 460px;
    .img-item {
      aspect-ratio: 4 / 3;
    }
  }
  &.grid-2 {
    grid-template-columns: repeat(2, 1fr);
    .img-item {
      aspect-ratio: 1;
    }
  }
  &.grid-3,
  &.grid-4,
  &.grid-5,
  &.grid-6,
  &.grid-7,
  &.grid-8,
  &.grid-9 {
    grid-template-columns: repeat(3, 1fr);
    .img-item {
      aspect-ratio: 1;
    }
  }

  .img-item {
    overflow: hidden;
    background: #f5f5f7;
    cursor: pointer;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.4s ease;
    }
    &:hover img {
      transform: scale(1.08);
    }
  }
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f3f3f7;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: none;
  background: transparent;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  color: #7a7a8a;
  transition: all 0.25s ease;

  .icon {
    font-size: 17px;
    line-height: 1;
  }

  &:hover {
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
    color: #ff4f8b;
  }

  &.active {
    color: #ff4f8b;
    background: #fff0f5;
    .icon {
      color: #ff4f8b;
    }
  }
}
</style>
