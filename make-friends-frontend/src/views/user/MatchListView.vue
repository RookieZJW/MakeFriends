<template>
  <div class="match-view">
    <div class="mf-container">
      <div class="match-header">
        <div>
          <h1 class="page-title"><span class="bar"></span>缘分匹配</h1>
          <p class="page-sub">互相喜欢的人，就是命中注定 ♡</p>
        </div>
      </div>

      <!-- 标签切换 -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <span>{{ tab.label }}</span>
          <span class="count" v-if="counts[tab.key]">{{ counts[tab.key] }}</span>
        </button>
      </div>

      <!-- 列表 -->
      <div v-loading="loading" class="match-grid">
        <div class="match-card mf-card" v-for="u in list" :key="u.id" @click="goDetail(u)">
          <div class="card-cover">
            <el-avatar :size="72" :src="resolveAvatar(u.avatar, u.nickname)" class="card-avatar" />
            <span class="gender-badge" :class="u.gender === 1 ? 'male' : 'female'">{{ u.gender === 1 ? '♂' : '♀' }}</span>
          </div>
          <div class="card-info">
            <div class="name-line">
              <span class="nickname">{{ u.nickname }}</span>
              <span class="age" v-if="u.age">{{ u.age }}岁</span>
            </div>
            <div class="meta">
              <span v-if="u.city">{{ u.city }}</span>
              <span v-if="u.occupation">· {{ u.occupation }}</span>
            </div>
            <p class="sig">{{ u.signature || '这个人很懒，什么都没留下~' }}</p>
          </div>
          <div class="card-actions" v-if="activeTab !== 'match'">
            <button class="like-action" @click.stop="onLike(u)">
              <span>♥</span> 喜欢
            </button>
          </div>
          <div class="card-actions" v-else>
            <button class="chat-action" @click.stop="goChat(u)">
              <el-icon><ChatDotRound /></el-icon> 聊天
            </button>
          </div>
        </div>

        <div v-if="!loading && list.length === 0" class="empty-state">
          <div class="empty-emoji">{{ activeTab === 'match' ? '💞' : '💌' }}</div>
          <p v-if="activeTab === 'match'">还没有互相匹配的人，去首页发现更多心动吧~</p>
          <p v-else-if="activeTab === 'myLike'">你还没有喜欢的人，去看看推荐吧~</p>
          <p v-else>还没有人喜欢你，完善资料让更多人看到你~</p>
          <button class="mf-btn" @click="$router.push('/home')">去发现</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getMatchList, getMyLikeList, getLikedMeList, likeUser, unlikeUser, getMatchCounts } from '@/api/match'
import { createOrGetSession } from '@/api/chat'
import { resolveAvatar } from '@/utils/format'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('match')
const list = ref([])
const counts = reactive({ match: 0, myLike: 0, likedMe: 0 })

const tabs = [
  { key: 'match', label: '互相匹配' },
  { key: 'myLike', label: '我喜欢的' },
  { key: 'likedMe', label: '喜欢我的' }
]

/** 拉取三类计数（进入页面即显示，不需要点 Tab 才有数字） */
async function loadCounts() {
  try {
    const res = await getMatchCounts()
    const data = res.data || res
    counts.match = Number(data.match) || 0
    counts.myLike = Number(data.myLike) || 0
    counts.likedMe = Number(data.likedMe) || 0
  } catch (e) {
    // 接口异常不影响列表展示
  }
}

async function loadData() {
  loading.value = true
  try {
    let api
    if (activeTab.value === 'match') api = getMatchList
    else if (activeTab.value === 'myLike') api = getMyLikeList
    else api = getLikedMeList

    const res = await api({ page: 1, pageSize: 50 })
    list.value = res.data && res.data.records ? res.data.records : res.data || []
    // 列表加载完成后也同步刷新一下当前 tab 的计数（保证准确）
    counts[activeTab.value] = res.data && res.data.total ? res.data.total : list.value.length
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  activeTab.value = key
  loadData()
}

function goDetail(u) {
  if (u.id) router.push(`/user/${u.id}`)
  else if (u.userId) router.push(`/user/${u.userId}`)
}

async function onLike(u) {
  try {
    await likeUser(u.id || u.userId)
    ElMessage.success('喜欢成功')
    // 点赞后实时刷新所有计数（我喜欢的 +1，可能互相匹配也 +1）
    loadCounts()
  } catch (e) {
    // 忽略
  }
}

async function goChat(u) {
  const userId = u.id || u.userId
  try {
    const res = await createOrGetSession(userId)
    const session = res.data || res
    const sessionId = session.id
    router.push(`/chat/${sessionId}`)
  } catch (e) {
    ElMessage.error('创建会话失败，请重试')
  }
}

onMounted(() => {
  // 进入页面就并行加载计数 + 列表
  loadCounts()
  loadData()
})

// keep-alive 缓存页面被重新激活时也刷新计数（从其他页面切回来数字是最新的）
onActivated(() => {
  loadCounts()
})
</script>

<style lang="scss" scoped>
.match-view {
  padding: 30px 0 40px;
}

.match-header {
  margin-bottom: 24px;
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

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;

  .tab {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 9px 22px;
    border: none;
    border-radius: 999px;
    background: #fff;
    color: #6a6a7a;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.25s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .count {
      min-width: 20px;
      height: 20px;
      padding: 0 6px;
      border-radius: 999px;
      background: #ffeef5;
      color: #ff4f8b;
      font-size: 12px;
      line-height: 20px;
      text-align: center;
    }

    &:hover {
      color: #ff4f8b;
    }

    &.active {
      background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
      color: #fff;
      box-shadow: 0 6px 16px rgba(255, 107, 157, 0.35);
      .count {
        background: rgba(255, 255, 255, 0.3);
        color: #fff;
      }
    }
  }
}

.match-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
  min-height: 200px;
}

.match-card {
  padding: 22px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 14px;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 32px rgba(168, 85, 247, 0.18);
  }
}

.card-cover {
  position: relative;
  display: flex;
  justify-content: center;

  .card-avatar {
    border: 3px solid #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  }

  .gender-badge {
    position: absolute;
    top: 0;
    right: calc(50% - 50px);
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    &.male { background: #3b82f6; }
    &.female { background: #ec4899; }
  }
}

.card-info {
  text-align: center;

  .name-line {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 4px;
    .nickname {
      font-size: 16px;
      font-weight: 700;
      color: #2d2d3a;
    }
    .age {
      font-size: 12px;
      color: #ff4f8b;
      font-weight: 600;
    }
  }

  .meta {
    font-size: 13px;
    color: #9a9aaa;
    margin-bottom: 6px;
  }

  .sig {
    margin: 0;
    font-size: 13px;
    color: #7a7a8a;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.card-actions {
  display: flex;
  justify-content: center;

  .like-action, .chat-action {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 7px 22px;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.25s ease;
  }

  .like-action {
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
    color: #ff4f8b;
    box-shadow: inset 0 0 0 1.5px rgba(255, 107, 157, 0.35);
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 18px rgba(255, 107, 157, 0.3);
    }
  }

  .chat-action {
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    color: #fff;
    box-shadow: 0 5px 14px rgba(255, 107, 157, 0.3);
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(168, 85, 247, 0.45);
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
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

/* ================== MatchListView 响应式 ================== */
@media (max-width: 1023px) {
  .match-view { padding: 20px 0 30px; }
  .page-title { font-size: 22px; .bar { height: 22px; } }
  .match-grid { grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)) !important; gap: 14px !important; }
}
@media (max-width: 767px) {
  .match-view { padding: 14px 0 24px; }
  .match-header { margin-bottom: 16px; }
  .page-title { font-size: 19px; gap: 8px; .bar { height: 18px; width: 4px; } }
  .page-sub { font-size: 13px; padding-left: 12px; }
  .tabs { gap: 6px; overflow-x: auto; flex-wrap: nowrap; padding-bottom: 4px; scrollbar-width: none; margin-bottom: 16px; }
  .tabs::-webkit-scrollbar { display: none; }
  .tabs .tab { padding: 8px 14px; font-size: 13px; flex-shrink: 0; }
  .match-grid { grid-template-columns: 1fr 1fr !important; gap: 10px !important; }
  .empty-state { padding: 48px 16px; .empty-emoji { font-size: 44px; } .mf-btn { width: 100%; } }
}
@media (max-width: 479px) {
  .page-title { font-size: 17px; }
  .page-sub { font-size: 12px; padding-left: 9px; }
  .match-grid { grid-template-columns: 1fr !important; }
}
</style>
