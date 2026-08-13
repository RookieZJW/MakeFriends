<template>
  <div class="dynamic-square">
    <div class="mf-container">
      <div class="square-head">
        <h1 class="page-title"><span class="bar"></span>动态广场</h1>
        <p class="page-sub">看看大家在分享什么有趣的事 ✨</p>
      </div>

      <!-- 推荐排序切换 -->
      <div class="sort-bar mf-card">
        <button
          v-for="s in sorts"
          :key="s.key"
          class="sort-btn"
          :class="{ active: sort === s.key }"
          @click="changeSort(s.key)"
        >
          <el-icon><component :is="s.icon" /></el-icon>
          {{ s.label }}
        </button>
      </div>

      <!-- 动态列表 -->
      <div class="dynamic-list" v-loading="loading" v-infinite-scroll="loadMore" :infinite-scroll-disabled="!hasMore || loading" :infinite-scroll-distance="100">
        <DynamicCard
          v-for="d in list"
          :key="d.id"
          :data="d"
          @deleted="onDeleted"
        />
      </div>

      <div v-if="loading" class="loading-tip">加载中...</div>
      <div v-else-if="!hasMore && list.length" class="loading-tip">— 已经到底啦 —</div>

      <div v-if="!loading && list.length === 0" class="empty-state">
        <div class="empty-emoji">📭</div>
        <p>广场还很安静，来发布第一条动态吧~</p>
      </div>
    </div>

    <!-- 悬浮发布按钮 -->
    <button class="fab" @click="$router.push('/dynamic/publish')">
      <el-icon><Plus /></el-icon>
      <span>发布</span>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Star, Clock } from '@element-plus/icons-vue'
import DynamicCard from '@/components/DynamicCard.vue'
import { getDynamicList } from '@/api/dynamic'

const loading = ref(false)
const list = ref([])
const sort = ref('new')
const page = ref(1)
const pageSize = 10
const hasMore = ref(true)

const sorts = [
  { key: 'new', label: '最新', icon: Clock },
  { key: 'hot', label: '热门', icon: Star }
]

async function loadData(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const res = await getDynamicList({ page: page.value, pageSize, sort: sort.value })
    const data = res.data || res
    const records = data.records || data || []
    if (reset) {
      list.value = records
    } else {
      list.value = list.value.concat(records)
    }
    const total = data.total || records.length
    hasMore.value = list.value.length < total && records.length > 0
    if (records.length > 0) page.value++
  } catch (e) {
    if (reset) list.value = mockList()
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (hasMore.value && !loading.value) loadData()
}

function changeSort(key) {
  if (sort.value === key) return
  sort.value = key
  loadData(true)
}

function onDeleted(id) {
  list.value = list.value.filter((d) => d.id !== id)
}

function mockList() {
  const names = ['林夕', '苏沐', '夏安', '顾川']
  return names.map((n, i) => ({
    id: 2000 + i,
    userId: 1000 + i,
    nickname: n,
    avatar: '',
    content: '今天天气真好，去公园散步啦~ 阳光温暖，心情也跟着明朗起来。生活就是要发现这些小确幸呀 ☀️',
    images: [],
    likeCount: Math.floor(Math.random() * 50),
    commentCount: Math.floor(Math.random() * 10),
    liked: false,
    createdAt: new Date(Date.now() - i * 3600000).toISOString()
  }))
}

onMounted(() => {
  loadData(true)
})
</script>

<style lang="scss" scoped>
.dynamic-square {
  padding: 30px 0 80px;
  position: relative;
}

.square-head {
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

.sort-bar {
  display: flex;
  gap: 8px;
  padding: 8px;
  margin-bottom: 22px;

  .sort-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 8px 20px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: #7a7a8a;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      background: #fff0f5;
      color: #ff4f8b;
    }

    &.active {
      background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
      color: #fff;
      box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
    }
  }
}

.dynamic-list {
  min-height: 200px;
}

.loading-tip {
  text-align: center;
  padding: 20px;
  color: #aaa;
  font-size: 13px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #aaa;

  .empty-emoji {
    font-size: 56px;
    margin-bottom: 12px;
  }
}

.fab {
  position: fixed;
  right: 40px;
  bottom: 40px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 14px 26px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 26px rgba(255, 107, 157, 0.45);
  transition: all 0.3s ease;
  z-index: 50;

  .el-icon {
    font-size: 18px;
  }

  &:hover {
    transform: translateY(-3px) scale(1.04);
    box-shadow: 0 14px 32px rgba(168, 85, 247, 0.5);
  }
}

/* ================== DynamicSquare 响应式 ================== */
@media (max-width: 1023px) {
  .dynamic-square { padding: 20px 0 30px; }
  .page-title { font-size: 22px; }
  .fab { right: 24px; bottom: 24px; padding: 12px 20px; }
  .tab-bar { gap: 6px; overflow-x: auto; scrollbar-width: none; flex-wrap: nowrap; padding-bottom: 4px; }
  .tab-bar::-webkit-scrollbar { display: none; }
}
@media (max-width: 767px) {
  .dynamic-square { padding: 14px 0 24px; }
  .page-head { margin-bottom: 14px; }
  .page-title { font-size: 19px; gap: 8px; .bar { height: 18px; width: 4px; } }
  .page-sub { font-size: 13px; padding-left: 12px; }
  .tab-bar .tab { padding: 8px 14px; font-size: 13px; flex-shrink: 0; }
  .dynamic-list {
    grid-template-columns: 1fr 1fr !important;
    gap: 10px !important;
  }
  .fab {
    right: 14px; bottom: 14px;
    padding: 10px 16px; font-size: 13.5px;
    .el-icon { font-size: 16px; }
  }
  .empty-state { padding: 40px 16px; .empty-emoji { font-size: 40px; } }
}
@media (max-width: 479px) {
  .page-title { font-size: 17px; }
  .page-sub { font-size: 12px; padding-left: 9px; }
  .dynamic-list { grid-template-columns: 1fr !important; }
  .fab {
    right: 12px; bottom: calc(12px + env(safe-area-inset-bottom));
    span:not(.el-icon) { display: none; }
    padding: 12px;
  }
}
</style>
