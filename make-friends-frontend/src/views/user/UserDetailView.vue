<template>
  <div class="user-detail-view" v-loading="loading">
    <!-- 封面 + 头像 -->
    <div class="profile-header">
      <div class="cover">
        <div class="cover-mask"></div>
      </div>
      <div class="profile-inner mf-container">
        <el-avatar :size="120" :src="avatarUrl" class="avatar" />
        <div class="profile-info">
          <div class="name-line">
            <h2 class="nickname">{{ user.nickname || '神秘用户' }}</h2>
            <span class="gender-tag" :class="genderClass">{{ genderEmoji(user.gender) }} {{ genderText(user.gender) }}</span>
            <span class="age-tag" v-if="user.age">{{ user.age }}岁</span>
          </div>
          <p class="signature">{{ user.signature || '这个人很懒，什么都没留下~' }}</p>
          <div class="meta-line">
            <span v-if="user.city"><el-icon><Location /></el-icon>{{ user.city }}</span>
            <span v-if="user.occupation"><el-icon><Briefcase /></el-icon>{{ user.occupation }}</span>
            <span v-if="user.height"><el-icon><Histogram /></el-icon>{{ user.height }}cm</span>
            <span v-if="user.weight"><el-icon><ScaleToOriginal /></el-icon>{{ user.weight }}kg</span>
            <span v-if="user.birthday"><el-icon><Calendar /></el-icon>{{ formatDate(user.birthday) }}</span>
          </div>
        </div>
        <div class="actions">
          <button class="mf-btn" :class="{ liked }" @click="onLike">
            <span class="heart">{{ liked ? '♥' : '♡' }}</span>{{ liked ? '已喜欢' : '喜欢' }}
          </button>
          <button class="mf-btn is-ghost" @click="startChat">
            <el-icon><ChatDotRound /></el-icon>发消息
          </button>
        </div>
      </div>
    </div>

    <div class="mf-container">
      <!-- 爱好标签 -->
      <div class="hobby-section mf-card" v-if="hobbies.length">
        <h3 class="block-title">兴趣爱好</h3>
        <div class="hobby-tags">
          <span class="hobby-tag" v-for="h in hobbies" :key="h">{{ h }}</span>
        </div>
      </div>

      <!-- TA 的动态 -->
      <div class="section-head">
        <h3 class="section-title"><span class="bar"></span>TA的动态</h3>
      </div>

      <div class="dynamic-list" v-if="dynamicList.length">
        <DynamicCard
          v-for="d in dynamicList"
          :key="d.id"
          :data="d"
        />
      </div>
      <div class="empty-state" v-else-if="!loading">
        <div class="empty-emoji">🗓️</div>
        <p>TA还没有发布动态哦~</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Location, Briefcase, Histogram, ScaleToOriginal, Calendar, ChatDotRound } from '@element-plus/icons-vue'
import DynamicCard from '@/components/DynamicCard.vue'
import { getUserById } from '@/api/user'
import { getUserDynamic } from '@/api/dynamic'
import { likeUser, unlikeUser } from '@/api/match'
import { sendMessage } from '@/api/chat'
import { genderText, genderEmoji, resolveAvatar, parseHobbies, formatDate } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { isLiked, addLiked, removeLiked } from '@/utils/likeCache'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const user = ref({})
const liked = ref(false)
const dynamicList = ref([])

const userId = computed(() => route.params.id)
const avatarUrl = computed(() => resolveAvatar(user.value.avatar, user.value.nickname))
const genderClass = computed(() => {
  const g = user.value.gender
  if (g === 1) return 'male'
  if (g === 2) return 'female'
  return 'secret'
})
const hobbies = computed(() => parseHobbies(user.value.hobbies))

async function loadData() {
  loading.value = true
  try {
    const res = await getUserById(userId.value)
    user.value = res.data || res
    // 双保险：后端 liked 字段 || 本地缓存（刷新后不丢失）
    liked.value = !!(user.value.liked || isLiked(userId.value, userStore.userId))
    try {
      const dRes = await getUserDynamic(userId.value, { page: 1, pageSize: 20 })
      dynamicList.value = dRes.data && dRes.data.records ? dRes.data.records : dRes.data || []
    } catch (e) {
      dynamicList.value = []
    }
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

async function onLike() {
  try {
    if (liked.value) {
      await unlikeUser(userId.value)
      liked.value = false
      removeLiked(userId.value, userStore.userId)
      ElMessage.success('已取消喜欢')
    } else {
      await likeUser(userId.value)
      liked.value = true
      addLiked(userId.value, userStore.userId)
      ElMessage.success('喜欢成功，缘分+1')
    }
  } catch (e) {
    // 忽略
  }
}

async function startChat() {
  try {
    const res = await sendMessage({ receiverId: userId.value, msgType: 1, content: '你好' })
    const sessionId = (res.data && res.data.sessionId) || (res.data && res.data.id) || res.data || res.id
    router.push(`/chat/${sessionId}`)
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.user-detail-view {
  padding-bottom: 40px;
}

.profile-header {
  position: relative;
  margin-bottom: 24px;
}

.cover {
  height: 220px;
  background: linear-gradient(135deg, #ff9ec4 0%, #c4a8ff 60%, #a855f7 100%);
  position: relative;
  .cover-mask {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 70% 30%, rgba(255, 255, 255, 0.3), transparent 50%);
  }
}

.profile-inner {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 24px;
  margin-top: -60px;

  .avatar {
    border: 5px solid #fff;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    flex-shrink: 0;
    background: #fff;
  }
}

.profile-info {
  flex: 1;
  padding-bottom: 8px;

  .name-line {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;

    .nickname {
      font-size: 24px;
      font-weight: 800;
      color: #2d2d3a;
      margin: 0;
    }
    .gender-tag, .age-tag {
      padding: 2px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      color: #fff;
    }
    .gender-tag.male { background: #3b82f6; }
    .gender-tag.female { background: #ec4899; }
    .gender-tag.secret { background: #94a3b8; }
    .age-tag { background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%); }
  }

  .signature {
    margin: 0 0 10px;
    font-size: 14px;
    color: #6a6a7a;
  }

  .meta-line {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 13px;
    color: #8a8a9a;
    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.actions {
  display: flex;
  gap: 10px;
  padding-bottom: 8px;
  flex-shrink: 0;

  .mf-btn.liked {
    background: linear-gradient(135deg, #ff4f8b 0%, #9333ea 100%);
  }
  .heart {
    margin-right: 2px;
  }
}

.hobby-section {
  padding: 22px 24px;
  margin-bottom: 24px;

  .block-title {
    font-size: 16px;
    font-weight: 700;
    color: #2d2d3a;
    margin: 0 0 14px;
  }
}

.hobby-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  .hobby-tag {
    padding: 6px 16px;
    border-radius: 999px;
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
    color: #a855f7;
    font-size: 13px;
    font-weight: 500;
    box-shadow: inset 0 0 0 1px rgba(168, 85, 247, 0.15);
  }
}

.section-head {
  margin: 10px 0 20px;
  .section-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    font-weight: 800;
    color: #2d2d3a;
    margin: 0;
    .bar {
      width: 5px;
      height: 20px;
      border-radius: 3px;
      background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    }
  }
}

.empty-state {
  text-align: center;
  padding: 50px 0;
  color: #aaa;
  .empty-emoji {
    font-size: 56px;
    margin-bottom: 12px;
  }
}

/* ================== UserDetailView 响应式 ================== */
@media (max-width: 1023px) {
  .user-detail { padding: 20px 0 30px; }
  .detail-hero { flex-direction: column; align-items: flex-start; gap: 16px; }
  .detail-grid { grid-template-columns: repeat(2, 1fr) !important; gap: 14px !important; }
}
@media (max-width: 767px) {
  .user-detail { padding: 14px 0 24px; }
  .detail-hero {
    flex-direction: column; align-items: center; text-align: center; gap: 12px; padding: 16px !important;
    .detail-avatar { --el-avatar-size: 80px !important; }
    .detail-info { align-items: center; }
    .detail-actions { width: 100%; flex-direction: row; }
    .detail-actions .mf-btn { flex: 1; }
  }
  .detail-grid { grid-template-columns: 1fr !important; gap: 10px !important; }
  .section-head { margin: 16px 0 12px; .section-title { font-size: 17px; .bar { height: 16px; } } }
  .photo-grid { gap: 8px; }
  .empty-state { padding: 36px 16px; .empty-emoji { font-size: 40px; } }
}
@media (max-width: 479px) {
  .detail-hero { .detail-avatar { --el-avatar-size: 68px !important; } .detail-name { font-size: 18px; } }
  .detail-actions { flex-direction: column !important; .mf-btn { width: 100%; } }
  .tag-cloud-wrap { gap: 6px; }
}
</style>
