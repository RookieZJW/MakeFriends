<template>
  <div class="home-view">
    <!-- 全局科技感背景装饰 -->
    <div class="bg-decor" aria-hidden="true">
      <div class="geo geo-1"></div>
      <div class="geo geo-2"></div>
      <div class="geo geo-3"></div>
    </div>
    <!-- Hero 横幅 -->
    <section class="hero">
      <div class="hero-inner mf-container">
        <div class="hero-text">
          <h1 class="hero-title">遇见<span class="gradient-text">心动</span>的那个人</h1>
          <p class="hero-desc">{{ greeting }}，{{ userStore.nickname }}！今天也要相信缘分的安排 ✨</p>
          <div class="hero-tags">
            <span class="tag">真实认证</span>
            <span class="tag">智能匹配</span>
            <span class="tag">隐私保护</span>
          </div>
        </div>
        <div class="hero-illu">
          <div class="float-heart h1">♡</div>
          <div class="float-heart h2">♥</div>
          <div class="float-heart h3">❤</div>
          <span class="hex hex-1"></span>
          <span class="hex hex-2"></span>
          <span class="ring-2"></span>
          <!-- 当前用户头像（带粉蓝渐变光晕+虚线旋转环） -->
          <div class="avatar-wrap">
            <div class="avatar-glow"></div>
            <img
              v-if="userStore.avatar"
              class="user-avatar"
              :src="resolveAvatarSrc(userStore.avatar)"
              :alt="userStore.nickname"
            />
            <div v-else class="avatar-fallback">
              {{ avatarInitial }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 筛选条 -->
    <div class="filter-bar mf-container">
      <div class="filter-card mf-card">
        <div class="filter-row">
          <div class="filter-item">
            <span class="label">性别</span>
            <el-select v-model="filter.gender" placeholder="不限" clearable size="large" style="width:100px">
              <el-option label="小哥哥" :value="1" />
              <el-option label="小仙女" :value="2" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="label">城市</span>
            <el-input v-model="filter.city" placeholder="输入城市" clearable size="large" style="width:140px" :prefix-icon="Location" />
          </div>
          <div class="filter-item">
            <span class="label">年龄</span>
            <el-select v-model="filter.ageRange" placeholder="不限" clearable size="large" style="width:110px">
              <el-option label="18-25岁" value="18-25" />
              <el-option label="26-30岁" value="26-30" />
              <el-option label="31-35岁" value="31-35" />
              <el-option label="35岁以上" value="35-99" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="label">职业</span>
            <el-select v-model="filter.occupation" placeholder="不限" clearable size="large" style="width:120px" filterable>
              <el-option v-for="o in occupationOptions" :key="o.name" :label="o.name" :value="o.name" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="label">爱好</span>
            <el-select v-model="filter.hobby" placeholder="不限" clearable size="large" style="width:120px" filterable>
              <el-option v-for="h in hobbyOptions" :key="h.name" :label="h.name" :value="h.name" />
            </el-select>
          </div>
          <button class="mf-btn search-btn" @click="loadUsers(true)">
            <el-icon><Search /></el-icon>搜索
          </button>
          <button class="mf-btn is-ghost reset-btn" @click="resetFilter">重置</button>
        </div>
      </div>
    </div>

    <!-- 用户网格 -->
    <div class="mf-container">
      <div class="section-head">
        <h2 class="section-title"><span class="bar"></span>为你推荐</h2>
        <span class="section-sub">{{ userList.length }} 位有趣的人在等你</span>
      </div>

      <div v-loading="loading" class="user-grid">
        <UserCard
          v-for="u in userList"
          :key="u.id"
          :user="u"
          @liked="onUserLiked"
          @unliked="onUserUnliked"
        />
        <div v-if="!loading && userList.length === 0" class="empty-state">
          <div class="empty-emoji">🔍</div>
          <p>暂无推荐用户，换个筛选条件试试~</p>
        </div>
      </div>

      <div class="load-more" v-if="hasMore && !loading">
        <button class="mf-btn is-ghost" @click="loadUsers()">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Location } from '@element-plus/icons-vue'
import UserCard from '@/components/UserCard.vue'
import { getRecommendUsers, searchUsers } from '@/api/user'
import { getDictAll } from '@/api/dict'
import { useUserStore } from '@/stores/user'
import { applyLikedToUsers } from '@/utils/likeCache'

const userStore = useUserStore()
const userList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const hasMore = ref(true)

const filter = reactive({
  gender: '',
  city: '',
  ageRange: '',
  occupation: '',
  hobby: ''
})

const occupationOptions = ref([])
const hobbyOptions = ref([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 取昵称第一个字/字母作为无头像时的兜底显示
const avatarInitial = computed(() => {
  const n = userStore.nickname || 'U'
  return n.trim().charAt(0).toUpperCase()
})
function resolveAvatarSrc(src) {
  if (!src) return ''
  if (src.startsWith('http') || src.startsWith('data:')) return src
  if (src.startsWith('/')) return src
  return '/' + src
}

function setUserLikedById(userId, liked) {
  const uid = String(userId)
  const target = userList.value.find(u => String(u && u.id) === uid)
  if (target) target.liked = !!liked
}
function onUserLiked(userId) { setUserLikedById(userId, true) }
function onUserUnliked(userId) { setUserLikedById(userId, false) }

async function loadUsers(reset = false) {
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize,
      gender: filter.gender || undefined,
      city: filter.city || undefined,
      occupation: filter.occupation || undefined,
      hobby: filter.hobby || undefined
    }
    if (filter.ageRange) {
      const [minStr, maxStr] = String(filter.ageRange).split('-')
      const min = parseInt(minStr, 10)
      const max = parseInt(maxStr, 10)
      if (!isNaN(min)) params.minAge = min
      if (!isNaN(max)) params.maxAge = max
    }
    const res = await getRecommendUsers(params)
    const list = res.data && res.data.records ? res.data.records : res.data || res.records || []
    // ✅ 关键修复：接口列表拉回来后，先把本地已缓存的「心动」状态回填上
    applyLikedToUsers(list, userStore.userId)
    const total = res.data && res.data.total ? res.data.total : res.total || list.length
    if (reset) {
      userList.value = list
    } else {
      userList.value = userList.value.concat(list)
    }
    hasMore.value = userList.value.length < total && list.length > 0
    if (list.length > 0) page.value++
  } catch (e) {
    if (reset) {
      const mocks = mockUsers()
      // 兜底 mock 数据也要带上心动状态
      applyLikedToUsers(mocks, userStore.userId)
      userList.value = mocks
    }
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filter.gender = ''
  filter.city = ''
  filter.ageRange = ''
  filter.occupation = ''
  filter.hobby = ''
  loadUsers(true)
}

function mockUsers() {
  const names = ['林夕', '苏沐', '陈默', '夏安', '顾川', '温言', '江晚', '陆星', '南风', '白栀', '余笙', '乔伊']
  const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京']
  const jobs = ['设计师', '程序员', '教师', '医生', '摄影师', '产品经理', '运营', '插画师']
  const sigs = ['愿你成为自己的太阳，无需凭借谁的光。', '生活明朗，万物可爱。', '保持热爱，奔赴山海。', '有趣灵魂，认真生活。', '愿有岁月可回首。', '做个温柔的普通人。']
  return names.slice(0, 10).map((n, i) => ({
    id: 1000 + i,
    nickname: n,
    gender: i % 2 === 0 ? 2 : 1,
    age: 20 + (i % 12),
    city: cities[i % cities.length],
    occupation: jobs[i % jobs.length],
    signature: sigs[i % sigs.length],
    avatar: ''
  }))
}

async function loadDictOptions() {
  try {
    const res = await getDictAll()
    const data = res.data || res
    if (data && data.occupations) {
      occupationOptions.value = data.occupations
    }
    if (data && data.hobbies) {
      hobbyOptions.value = data.hobbies
    }
  } catch (e) {
  }
}

onMounted(async () => {
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
  loadDictOptions()
  loadUsers(true)
})
</script>

<style lang="scss" scoped>
.home-view {
  padding-bottom: 40px;
  position: relative;
  z-index: 2;
}

/* Hero 区 */
.hero {
  background: transparent;
  padding: 48px 0 60px;
  position: relative;
}

.hero-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
}

.hero-text {
  flex: 1;

  .hero-title {
    font-size: 36px;
    font-weight: 700;
    margin: 0 0 14px;
    color: #2F3443;
    letter-spacing: 0.5px;
  }

  .hero-desc {
    font-size: 16px;
    color: #6B7280;
    margin: 0 0 20px;
  }

  .hero-tags {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;

    .tag {
      padding: 6px 16px;
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      border-radius: 999px;
      font-size: 13px;
      color: #5B8DEF;
      font-weight: 500;
      border: 1px solid rgba(91, 141, 239, 0.15);
    }
  }
}

.hero-illu {
  position: relative;
  width: 180px;
  height: 180px;
  flex-shrink: 0;

    .float-heart {
    position: absolute;
    font-size: 28px;
    color: #F08DA5;
  }
  .h1 { top: 0; left: 10%; }
  .h2 { top: 40%; right: 0; color: #5B8DEF; }
  .h3 { bottom: 0; left: 30%; }

  /* 用户头像容器 */
  .avatar-wrap {
    position: absolute;
    inset: 26px;
    border-radius: 50%;
    overflow: hidden;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.9),
                0 8px 32px rgba(91, 141, 239, 0.25);
  }
  .avatar-glow {
    position: absolute;
    inset: -2px;
    border-radius: 50%;
    background: linear-gradient(135deg, #5B8DEF 0%, #FFB5C5 100%);
    opacity: 0.22;
    filter: blur(2px);
    animation: pulse-glow 6s ease-in-out infinite;
    z-index: 0;
  }
  .user-avatar {
    position: relative;
    z-index: 1;
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
  .avatar-fallback {
    position: relative;
    z-index: 1;
    width: 100%;
    height: 100%;
    display:flex;
    align-items: center;
    justify-content: center;
    font-size: 44px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(135deg, #7FA6F5 0%, #FFC1CF 100%);
    letter-spacing: 1px;
  }
}
@keyframes pulse-glow {
  0%, 100% { opacity: 0.22; transform: scale(1); }
  50%      { opacity: 0.38; transform: scale(1.04); }
}

/* 筛选条 */
.filter-bar {
  margin-top: -20px;
  position: relative;
  z-index: 5;
}

.filter-card {
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(91, 141, 239, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;

  :deep(.el-select__wrapper),
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.85) !important;
  }
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .label {
    font-size: 14px;
    font-weight: 500;
    color: #2F3443;
    white-space: nowrap;
  }
}

.search-btn, .reset-btn {
  padding: 8px 22px;
  font-size: 14px;
}

/* 标题区 */
.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 32px 0 20px;
  padding: 0 4px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    font-weight: 700;
    color: #2F3443;
    margin: 0;

    .bar {
      width: 4px;
      height: 20px;
      border-radius: 2px;
      background: linear-gradient(180deg, #5B8DEF 0%, #FFB5C5 100%);
    }
  }

  .section-sub {
    font-size: 14px;
    color: #6B7280;
  }
}

/* 用户网格 */
.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #9CA3AF;

  .empty-emoji {
    font-size: 48px;
    margin-bottom: 12px;
  }
}

.load-more {
  text-align: center;
  margin-top: 32px;

  .mf-btn.is-ghost {
    background: rgba(255, 255, 255, 0.8) !important;
    backdrop-filter: blur(8px);
  }
}

/* ========== 科技感背景装饰 ========== */
.bg-decor {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}
.geo {
  position: absolute;
  border: 1px solid rgba(91, 141, 239, 0.18);
  border-radius: 50%;
  animation: geo-drift 22s linear infinite;
}
.geo-1 {
  width: 320px;
  height: 320px;
  top: -80px;
  left: -80px;
  border-top-color: rgba(255, 181, 197, 0.25);
  animation-duration: 28s;
}
.geo-2 {
  width: 420px;
  height: 420px;
  bottom: -120px;
  right: -140px;
  border-left-color: rgba(91, 141, 239, 0.28);
  border-bottom-color: rgba(240, 141, 165, 0.18);
  animation-duration: 34s;
  animation-direction: reverse;
}
.geo-3 {
  width: 260px;
  height: 260px;
  top: 40%;
  left: 60%;
  border-right-color: rgba(91, 141, 239, 0.3);
  animation-duration: 26s;
  animation-delay: -6s;
}
@keyframes geo-drift {
  0%   { transform: rotate(0deg) translateY(0); }
  50%  { transform: rotate(180deg) translateY(12px); }
  100% { transform: rotate(360deg) translateY(0); }
}

/* Hero 区几何 */
.hero-illu {
  position: relative;
  width: 180px;
  height: 180px;
  flex-shrink: 0;

  .ring {
    position: absolute;
    inset: 30px;
    border-radius: 50%;
    background: linear-gradient(135deg, #5B8DEF, #FFB5C5);
    opacity: 0.15;
    animation: pulse-ring 6s ease-in-out infinite;
  }
  .ring-2 {
    position: absolute;
    inset: 10px;
    border-radius: 50%;
    border: 1px dashed rgba(91, 141, 239, 0.35);
    animation: spin-slow 18s linear infinite;
  }

  .float-heart {
    position: absolute;
    font-size: 28px;
    color: #F08DA5;
    animation: float-heart 4s ease-in-out infinite;
  }
  .h1 { top: 0; left: 10%; animation-delay: 0s; }
  .h2 { top: 40%; right: 0; color: #5B8DEF; animation-delay: -1s; }
  .h3 { bottom: 0; left: 30%; animation-delay: -2s; }

  .hex {
    position: absolute;
    width: 18px;
    height: 18px;
    border: 1.5px solid rgba(91, 141, 239, 0.45);
    clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  }
  .hex-1 {
    top: 12%; left: 24%;
    border-color: rgba(255, 181, 197, 0.6);
    background: rgba(255, 181, 197, 0.2);
    animation: hex-float 6s ease-in-out infinite;
  }
  .hex-2 {
    bottom: 18%; right: 20%;
    border-color: rgba(91, 141, 239, 0.65);
    background: rgba(91, 141, 239, 0.18);
    animation: hex-float 7s ease-in-out infinite -3s;
  }
}

@keyframes pulse-ring {
  0%, 100% { transform: scale(1); opacity: 0.15; }
  50%      { transform: scale(1.08); opacity: 0.22; }
}
@keyframes spin-slow {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
@keyframes float-heart {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}
@keyframes hex-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-8px) rotate(20deg); }
}

/* ================== HomeView 响应式 ================== */
@media (max-width: 1023px) {
  .hero { padding: 32px 0 40px; }
  .hero-inner { gap: 24px; flex-direction: column; align-items: flex-start; }
  .hero-text .hero-title { font-size: 28px; }
  .hero-text .hero-desc { font-size: 15px; }
  .hero-illu { width: 140px; height: 140px; order: -1; margin: 0 auto;
    .avatar-wrap { inset: 20px; }
    .avatar-fallback { font-size: 34px; }
  }
  .user-grid { gap: 14px; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); }
  .filter-card { padding: 14px 16px; }
}
@media (max-width: 767px) {
  .home-view { padding-bottom: 28px; }
  .hero { padding: 20px 0 28px; }
  .hero-text .hero-title { font-size: 22px; margin-bottom: 10px; }
  .hero-text .hero-desc { font-size: 14px; margin-bottom: 14px; }
  .hero-tags .tag { padding: 5px 12px; font-size: 12px; }
  .hero-illu { width: 110px; height: 110px;
    .float-heart { font-size: 22px; }
    .avatar-wrap { inset: 16px; }
    .avatar-fallback { font-size: 26px; }
  }
  .filter-bar { margin-top: -12px; }
  .filter-card { padding: 12px; border-radius: 12px; }
  .filter-row { gap: 10px; }
  .filter-item { width: 100%; }
  .filter-item .label { min-width: 52px; }
  .search-btn, .reset-btn { width: 100%; }
  .section-head { margin: 22px 0 14px; .section-title { font-size: 17px; .bar { height: 16px; } } }
  .user-grid {
    grid-template-columns: 1fr 1fr;  /* 手机两列更紧凑 */
    gap: 10px;
  }
  .load-more .mf-btn { width: 100%; }
  .empty-state { padding: 40px 0; .empty-emoji { font-size: 36px; } }
}
@media (max-width: 479px) {
  .hero-text .hero-title { font-size: 19px; }
  .hero-text .hero-desc { font-size: 13px; }
  .user-grid { grid-template-columns: 1fr; }  /* 小手机单列 */
  .filter-item { flex-direction: column; align-items: stretch; gap: 4px; }
}
</style>