<template>
  <div class="profile-view" v-loading="loading">
    <!-- ============ Hero Section ============ -->
    <section class="hero">
      <div class="hero-bg">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
        <div class="blob blob-3"></div>
        <div class="grid-overlay"></div>
      </div>

      <div class="hero-content mf-container">
        <!-- Avatar with floating glow ring -->
        <div class="avatar-wrap">
          <div class="avatar-glow"></div>
          <el-avatar :size="128" :src="avatarUrl" class="avatar" />
          <div class="online-badge" :class="statusClass">
            <span class="badge-dot"></span>
          </div>
        </div>

        <!-- Profile Info -->
        <div class="profile-info">
          <div class="name-row">
            <h2 class="nickname">{{ user.nickname || '游客' }}</h2>
            <span class="tag tag-gender" :class="genderClass">
              {{ genderEmoji(user.gender) }} {{ genderText(user.gender) }}
            </span>
            <span class="tag tag-age" v-if="user.age">{{ user.age }}岁</span>
          </div>

          <p class="signature">{{ user.signature || '这个人很懒，什么都没留下~' }}</p>

          <div class="info-chips">
            <span class="chip" v-if="user.city">
              <el-icon><Location /></el-icon>
              {{ user.city }}
            </span>
            <span class="chip" v-if="user.occupation">
              <el-icon><Briefcase /></el-icon>
              {{ user.occupation }}
            </span>
            <span class="chip" v-if="user.height">
              <el-icon><Histogram /></el-icon>
              {{ user.height }}cm
            </span>
            <span class="chip chip-hobby" v-if="hobbies.length">
              <el-icon><Star /></el-icon>
              {{ hobbies.join(' · ') }}
            </span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="hero-actions">
          <button class="action-btn btn-primary" @click="router.push('/profile/edit')">
            <el-icon><Edit /></el-icon>
            <span>编辑资料</span>
          </button>
          <button class="action-btn btn-secondary" @click="router.push('/dynamic/publish')">
            <el-icon><EditPen /></el-icon>
            <span>发布动态</span>
          </button>
        </div>
      </div>
    </section>

    <!-- ============ Stats + Status Row ============ -->
    <div class="mf-container">
      <div class="stats-row">
        <div class="stat-card" v-for="item in statCards" :key="item.key">
          <div class="stat-icon" :style="{ background: item.iconBg }">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
          <div class="stat-trend" v-if="item.trend">{{ item.trend }}</div>
        </div>
      </div>

      <!-- Online Status -->
      <div class="status-panel">
        <div class="status-icon" :class="statusClass">
          <span class="pulse-ring"></span>
          <el-icon><component :is="isInvisible ? 'View' : 'CircleCheckFilled'" /></el-icon>
        </div>
        <div class="status-info">
          <div class="status-title">{{ statusTitle }}</div>
          <div class="status-desc">{{ statusDesc }}</div>
        </div>
        <el-switch
          v-model="isInvisible"
          active-text="隐身模式"
          inactive-text="在线状态"
          inline-prompt
          :loading="statusLoading"
          @change="onToggleInvisible"
          style="--el-switch-on-color: #a855f7; --el-switch-off-color: #22c55e;"
        />
      </div>

      <!-- ============ Dynamics Section ============ -->
      <section class="dynamics-section">
        <div class="section-head">
          <div class="section-title-wrap">
            <span class="section-bar"></span>
            <h3 class="section-title">我的动态</h3>
            <span class="section-count">{{ stats.dynamic }}</span>
          </div>
          <router-link to="/dynamic" class="section-link">
            查看广场
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <div class="dynamic-list" v-if="dynamicList.length">
          <DynamicCard
            v-for="d in dynamicList"
            :key="d.id"
            :data="d"
            @deleted="onDeleted"
          />
        </div>

        <div class="empty-state" v-else-if="!loading">
          <div class="empty-visual">
            <div class="empty-circle"></div>
            <div class="empty-emoji">📝</div>
          </div>
          <p class="empty-text">还没有发布动态，去分享你的生活吧~</p>
          <button class="mf-btn" @click="router.push('/dynamic/publish')">
            <el-icon><EditPen /></el-icon>
            发布第一条动态
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Location, Briefcase, Histogram, Star, Edit, EditPen,
  View, CircleCheckFilled, ArrowRight, Document,
  StarFilled, UserFilled, User, Link
} from '@element-plus/icons-vue'
import DynamicCard from '@/components/DynamicCard.vue'
import { useUserStore } from '@/stores/user'
import { getUserDynamic } from '@/api/dynamic'
import { getMatchList, getMyLikeList, getLikedMeList } from '@/api/match'
import { setOnlineStatus, getUserInfo } from '@/api/user'
import { genderText, genderEmoji, resolveAvatar, parseHobbies } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const user = computed(() => userStore.userInfo || {})

const avatarUrl = computed(() => resolveAvatar(user.value.avatar, user.value.nickname))
const genderClass = computed(() => {
  const g = user.value.gender
  if (g === 1) return 'male'
  if (g === 2) return 'female'
  return 'secret'
})
const hobbies = computed(() => parseHobbies(user.value.hobbies))

const dynamicList = ref([])
const stats = reactive({
  dynamic: 0,
  likeCount: 0,
  likedCount: 0,
  matchCount: 0
})

const statCards = computed(() => [
  {
    key: 'dynamic',
    icon: Document,
    label: '动态',
    value: stats.dynamic,
    iconBg: 'linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%)',
    trend: null
  },
  {
    key: 'likeCount',
    icon: StarFilled,
    label: '我喜欢的',
    value: stats.likeCount,
    iconBg: 'linear-gradient(135deg, #f472b6 0%, #c084fc 100%)',
    trend: null
  },
  {
    key: 'likedCount',
    icon: UserFilled,
    label: '被喜欢',
    value: stats.likedCount,
    iconBg: 'linear-gradient(135deg, #fb923c 0%, #f472b6 100%)',
    trend: null
  },
  {
    key: 'matchCount',
    icon: Link,
    label: '互相匹配',
    value: stats.matchCount,
    iconBg: 'linear-gradient(135deg, #34d399 0%, #22c55e 100%)',
    trend: null
  }
])

async function loadData() {
  loading.value = true
  try {
    try {
      const res = await getUserDynamic(userStore.userId, { page: 1, pageSize: 10 })
      dynamicList.value = res.data && res.data.records ? res.data.records : res.data || []
      stats.dynamic = res.data && res.data.total ? res.data.total : dynamicList.value.length
    } catch (e) {
      dynamicList.value = []
    }
    try {
      const [matchRes, myLikeRes, likedMeRes] = await Promise.allSettled([
        getMatchList({ page: 1, pageSize: 1 }),
        getMyLikeList({ page: 1, pageSize: 1 }),
        getLikedMeList({ page: 1, pageSize: 1 })
      ])
      if (matchRes.status === 'fulfilled') stats.matchCount = (matchRes.value.data && matchRes.value.data.total) || 0
      if (myLikeRes.status === 'fulfilled') stats.likeCount = (myLikeRes.value.data && myLikeRes.value.data.total) || 0
      if (likedMeRes.status === 'fulfilled') stats.likedCount = (likedMeRes.value.data && likedMeRes.value.data.total) || 0
    } catch (e) {
    }
  } finally {
    loading.value = false
  }
}

function onDeleted(id) {
  dynamicList.value = dynamicList.value.filter((d) => d.id !== id)
  stats.dynamic = Math.max(0, stats.dynamic - 1)
}

const isInvisible = ref(false)
const statusLoading = ref(false)
const statusClass = computed(() => (isInvisible.value ? 'invisible' : 'online'))
const statusTitle = computed(() => (isInvisible.value ? '隐身中' : '在线'))
const statusDesc = computed(() =>
  isInvisible.value ? '他人看到你为离线状态，但可正常使用功能' : '其他用户可以看到你当前在线'
)

function syncOnlineStatus() {
  const s = userStore.userInfo && userStore.userInfo.onlineStatus
  isInvisible.value = Number(s) === 2
}
watch(() => userStore.userInfo, syncOnlineStatus, { immediate: true, deep: true })

async function onToggleInvisible(val) {
  statusLoading.value = true
  try {
    await setOnlineStatus(val ? 2 : 1)
    try {
      const infoRes = await getUserInfo()
      const info = infoRes.data || infoRes || {}
      if (userStore.setUserInfo) userStore.setUserInfo(info)
      else userStore.userInfo = { ...(userStore.userInfo || {}), ...info }
    } catch (err) {}
    ElMessage.success(val ? '已切换为隐身状态' : '已切换为在线状态')
  } catch (err) {
    isInvisible.value = !val
    ElMessage.error(err?.response?.data?.message || err?.message || '切换失败')
  } finally {
    statusLoading.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  syncOnlineStatus()
  loadData()
})
</script>

<style lang="scss" scoped>
.profile-view {
  padding-bottom: 40px;
  background: #F7F9FC;
}

/* ============ Hero ============ */
.hero {
  position: relative;
  overflow: hidden;
  padding: 0;
  margin-bottom: 28px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  height: 340px;
  background: linear-gradient(145deg, #ffc3e0 0%, #d4b5ff 50%, #a855f7 100%);
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
}
.blob-1 {
  width: 320px; height: 320px;
  background: #ff6b9d;
  top: -80px; left: -60px;
  animation: floatBlob1 12s ease-in-out infinite;
}
.blob-2 {
  width: 260px; height: 260px;
  background: #f0abfc;
  top: -40px; right: -40px;
  animation: floatBlob2 14s ease-in-out infinite;
}
.blob-3 {
  width: 200px; height: 200px;
  background: #818cf8;
  bottom: -60px; left: 30%;
  animation: floatBlob3 16s ease-in-out infinite;
}

@keyframes floatBlob1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 40px); }
}
@keyframes floatBlob2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, 30px); }
}
@keyframes floatBlob3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(40px, -20px); }
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 32px 32px;
}

.hero-content {
  position: relative;
  padding-top: 40px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 28px;
  align-items: end;
  padding-bottom: 32px;
}

/* Avatar */
.avatar-wrap {
  position: relative;
  margin-bottom: -20px;
}

.avatar-glow {
  position: absolute;
  inset: -12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #a855f7, #818cf8);
  z-index: -1;
  opacity: 0.55;
  animation: glow 4s ease-in-out infinite;
}

@keyframes glow {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.05); }
}

.avatar {
  border: 4px solid #fff;
  box-shadow: 0 12px 32px rgba(168, 85, 247, 0.35);
  background: #fff;
  transition: transform 0.3s ease;
  &:hover { transform: scale(1.03); }
}

.online-badge {
  position: absolute;
  bottom: 4px; right: 4px;
  width: 24px; height: 24px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  &.online {
    .badge-dot {
      width: 14px; height: 14px;
      border-radius: 50%;
      background: #22c55e;
      box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.3);
    }
  }
  &.invisible {
    .badge-dot {
      width: 14px; height: 14px;
      border-radius: 50%;
      background: #9ca3af;
    }
  }
}

/* Profile Info */
.profile-info {
  padding-bottom: 4px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.nickname {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  letter-spacing: 0.5px;
}

.tag {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  backdrop-filter: blur(8px);
}
.tag-gender.male { background: rgba(59, 130, 246, 0.85); }
.tag-gender.female { background: rgba(236, 72, 153, 0.85); }
.tag-gender.secret { background: rgba(148, 163, 184, 0.85); }
.tag-age { background: rgba(255, 255, 255, 0.25); border: 1px solid rgba(255, 255, 255, 0.4); }

.signature {
  margin: 0 0 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.6;
  max-width: 520px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.info-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
  font-size: 12.5px;
  color: #fff;
  font-weight: 500;

  .el-icon { font-size: 13px; }
}

/* Hero Actions */
.hero-actions {
  display: flex;
  gap: 10px;
  padding-bottom: 4px;
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 11px 22px;
  border-radius: 12px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.btn-primary {
    background: #fff;
    color: #a855f7;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
    }
  }
  &.btn-secondary {
    background: rgba(255, 255, 255, 0.15);
    color: #fff;
    border: 1px solid rgba(255, 255, 255, 0.4);
    backdrop-filter: blur(10px);
    &:hover {
      background: rgba(255, 255, 255, 0.25);
      transform: translateY(-2px);
    }
  }
}

/* ============ Stats Row ============ */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(91, 141, 239, 0.06);
  border: 1px solid rgba(91, 141, 239, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0; right: 0;
    width: 80px; height: 80px;
    background: linear-gradient(135deg, transparent 0%, rgba(168, 85, 247, 0.05) 100%);
    border-radius: 0 0 0 80px;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 28px rgba(91, 141, 239, 0.12);
  }
}

.stat-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-body { flex: 1; min-width: 0; }

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #2d2d3a;
  line-height: 1.1;
}

.stat-label {
  margin-top: 2px;
  font-size: 12.5px;
  color: #8a8a9a;
  font-weight: 500;
}

/* ============ Status Panel ============ */
.status-panel {
  background: #fff;
  border-radius: 16px;
  padding: 16px 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: 0 2px 12px rgba(91, 141, 239, 0.06);
  border: 1px solid rgba(91, 141, 239, 0.08);
  margin-bottom: 32px;
}

.status-icon {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
  font-size: 20px;

  &.online {
    background: linear-gradient(135deg, #22c55e, #16a34a);
    color: #fff;
    .pulse-ring {
      position: absolute;
      inset: -4px;
      border-radius: 50%;
      border: 2px solid rgba(34, 197, 94, 0.4);
      animation: pulse 2s ease-out infinite;
    }
  }
  &.invisible {
    background: linear-gradient(135deg, #9ca3af, #6b7280);
    color: #fff;
    .pulse-ring { display: none; }
  }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}

.status-info { flex: 1; }

.status-title {
  font-size: 15px;
  font-weight: 700;
  color: #2d2d3a;
}

.status-desc {
  font-size: 12.5px;
  color: #8a8a9a;
  margin-top: 2px;
}

/* ============ Dynamics Section ============ */
.dynamics-section { margin-top: 8px; }

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-bar {
  width: 4px;
  height: 22px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ff6b9d, #a855f7);
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: #2d2d3a;
  margin: 0;
}

.section-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 22px;
  padding: 0 8px;
  border-radius: 11px;
  background: linear-gradient(135deg, #ff6b9d, #a855f7);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #a855f7;
  transition: all 0.2s;
  text-decoration: none;
  &:hover {
    color: #9333ea;
    gap: 7px;
  }
  .el-icon { font-size: 14px; }
}

.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;

  .empty-text {
    margin: 16px 0 20px;
    color: #8a8a9a;
    font-size: 14px;
  }
}

.empty-visual {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
}

.empty-circle {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #fef3c7 0%, #fce7f3 100%);
}

.empty-emoji {
  position: relative;
  font-size: 40px;
}

.empty-state .mf-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* ============ Responsive ============ */
@media (max-width: 1023px) {
  .hero-bg { height: 300px; }
  .hero-content { gap: 20px; }
  .stats-row { grid-template-columns: repeat(2, 1fr); gap: 12px; }
}

@media (max-width: 767px) {
  .profile-view { padding-bottom: calc(24px + env(safe-area-inset-bottom, 0px)); }
  .hero-bg { height: 260px; }

  .hero-content {
    grid-template-columns: 1fr;
    gap: 14px;
    padding-top: 28px;
    text-align: center;
  }

  .avatar-wrap {
    margin: 0 auto;
    margin-bottom: -12px;
  }

  .profile-info { text-align: center; }
  .name-row { justify-content: center; }
  .signature { margin: 0 auto 10px; }
  .info-chips { justify-content: center; }

  .hero-actions {
    justify-content: center;
    flex-wrap: wrap;
    padding-bottom: 0;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .stat-card {
    padding: 14px;
    gap: 10px;
  }

  .stat-icon {
    width: 36px; height: 36px;
    font-size: 16px;
  }

  .stat-value { font-size: 20px; }

  .status-panel { padding: 14px 16px; gap: 14px; flex-wrap: wrap; }
  .status-info { flex: 1; min-width: 140px; }

  .section-title { font-size: 17px; }
  .section-title-wrap { gap: 8px; }
}

@media (max-width: 479px) {
  .hero-bg { height: 240px; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 8px; }
  .stat-value { font-size: 18px; }
  .stat-label { font-size: 11px; }

  .hero-actions { flex-direction: column; width: 100%; }
  .hero-actions .action-btn { width: 100%; justify-content: center; }

  .nickname { font-size: 22px; }
  .empty-state { padding: 40px 12px; }
}
</style>
