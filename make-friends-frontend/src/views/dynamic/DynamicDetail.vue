<template>
  <div class="dynamic-detail" v-loading="loading">
    <div class="mf-container">
      <div class="detail-wrap">
        <!-- 动态主体 -->
        <div class="detail-card mf-card" v-if="dynamic.id">
          <div class="card-header">
            <el-avatar :size="48" :src="resolveAvatar(dynamic.avatar, dynamic.nickname)" class="clickable" @click="goUser" />
            <div class="user-meta">
              <span class="username" @click="goUser">{{ dynamic.nickname || '匿名用户' }}</span>
              <span class="time">{{ formatRelativeTime(dynamic.createdAt) }} · {{ formatDateTime(dynamic.createdAt) }}</span>
            </div>
          </div>

          <div class="content">{{ dynamic.content }}</div>

          <div class="image-grid" v-if="imageList.length" :class="`grid-${imageList.length}`">
            <div v-for="(img, i) in imageList" :key="i" class="img-item" @click="previewIndex = i">
              <img :src="resolveImage(img)" alt="" />
            </div>
          </div>

          <el-image-viewer v-if="previewIndex >= 0" :url-list="previewList" :initial-index="previewIndex" @close="previewIndex = -1" />

          <div class="card-footer">
            <button class="action-btn" :class="{ active: liked }" @click="onLike">
              <span class="icon" :class="{ 'heart-beat': liked }">{{ liked ? '♥' : '♡' }}</span>
              <span>{{ likeCount }}</span>
            </button>
            <button class="action-btn">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ totalCommentCount }}</span>
            </button>
            <button class="action-btn" @click="onShare">
              <el-icon><Share /></el-icon><span>分享</span>
            </button>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="comment-section mf-card">
          <h3 class="section-title"><span class="bar"></span>评论 ({{ totalCommentCount }})</h3>

          <!-- 发表评论 -->
          <div class="comment-input">
            <el-avatar :size="38" :src="resolveAvatar(userStore.userInfo && userStore.userInfo.avatar, userStore.nickname)" />
            <el-input
              v-model="commentText"
              placeholder="说点什么吧..."
              @keyup.enter="onComment"
            />
            <button class="mf-btn" @click="onComment" :disabled="!commentText.trim()">发送</button>
          </div>

          <!-- 评论列表 -->
          <div class="comment-list" v-if="commentTree.length">
            <CommentItem
              v-for="c in commentTree"
              :key="c.id"
              :data="c"
              @reply="onReply"
            />
          </div>
          <div v-else class="empty-comment">
            还没有评论，快来抢沙发吧~
          </div>
        </div>
      </div>
    </div>

    <!-- 回复输入弹框 -->
    <el-dialog v-model="replyVisible" width="460px" class="reply-dialog">
      <template #header>
        <div class="reply-dialog-title">
          <span>回复</span>
          <span class="reply-target-hint" v-if="replyTarget">
            回复 <b>{{ replyTarget.nickname || '匿名用户' }}</b> 的评论
          </span>
        </div>
      </template>
      <el-input v-model="replyText" placeholder="写下你的回复..." type="textarea" :rows="3" />
      <template #footer>
        <button class="mf-btn is-ghost" @click="replyVisible = false">取消</button>
        <button class="mf-btn" @click="submitReply" :disabled="!replyText.trim()">回复</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Share } from '@element-plus/icons-vue'
import CommentItem from '@/components/CommentItem.vue'
import { getDynamicDetail, toggleLike, getComments, addComment } from '@/api/dynamic'
import { formatRelativeTime, formatDateTime, resolveAvatar, resolveImage } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dynamic = ref({})
const liked = ref(false)
const likeCount = ref(0)
const commentList = ref([])
const commentText = ref('')

const replyVisible = ref(false)
const replyText = ref('')
const replyTarget = ref(null)

const previewIndex = ref(-1)

const imageList = computed(() => {
  const imgs = dynamic.value.images || dynamic.value.imageList
  if (!imgs) return []
  if (Array.isArray(imgs)) return imgs
  return String(imgs).split(',').filter(Boolean)
})

const previewList = computed(() => imageList.value.map((i) => resolveImage(i)))

function buildCommentTree(flatList) {
  if (!flatList || !flatList.length) return []
  const map = {}
  flatList.forEach(item => { map[item.id] = { ...item, children: [] } })

  // 向上追溯到根评论（parentId 为 null 或 map 中不存在父节点的）
  function findRootId(id) {
    let cur = id
    while (true) {
      const node = map[cur]
      if (!node || !node.parentId || !map[node.parentId]) return cur
      cur = node.parentId
    }
  }

  const roots = []
  flatList.forEach(item => {
    const node = map[item.id]
    const isRoot = !item.parentId || !map[item.parentId]
    if (isRoot) {
      roots.push(node)
    } else {
      const rootId = findRootId(item.id)
      if (rootId !== item.id && map[rootId]) {
        map[rootId].children.push(node)
      } else {
        roots.push(node)
      }
    }
  })
  return roots
}

const commentTree = computed(() => buildCommentTree(commentList.value))

const totalCommentCount = computed(() => commentList.value.length)

async function loadDetail() {
  loading.value = true
  try {
    const res = await getDynamicDetail(route.params.id)
    dynamic.value = res.data || res
    liked.value = !!dynamic.value.liked
    likeCount.value = dynamic.value.likeCount || 0
    try {
      const cRes = await getComments(route.params.id)
      commentList.value = Array.isArray(cRes.data) ? cRes.data : (cRes.data && cRes.data.records ? cRes.data.records : [])
    } catch (e) {
      commentList.value = []
    }
  } catch (e) {
    ElMessage.error('动态不存在或已被删除')
  } finally {
    loading.value = false
  }
}

function goUser() {
  if (dynamic.value.userId) router.push(`/user/${dynamic.value.userId}`)
}

async function onLike() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    await toggleLike(dynamic.value.id)
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
  } catch (e) {
  }
}

async function onComment() {
  const text = commentText.value.trim()
  if (!text) return
  try {
    await addComment({ dynamicId: dynamic.value.id, content: text })
    ElMessage.success('评论成功')
    commentText.value = ''
    const cRes = await getComments(route.params.id)
    commentList.value = Array.isArray(cRes.data) ? cRes.data : (cRes.data && cRes.data.records ? cRes.data.records : [])
  } catch (e) {
  }
}

function onReply(comment) {
  replyTarget.value = comment
  replyText.value = ''
  replyVisible.value = true
}

async function submitReply() {
  const text = replyText.value.trim()
  if (!text || !replyTarget.value) return
  try {
    await addComment({
      dynamicId: dynamic.value.id,
      parentId: replyTarget.value.id,
      content: text
    })
    ElMessage.success('回复成功')
    replyVisible.value = false
    const cRes = await getComments(route.params.id)
    commentList.value = Array.isArray(cRes.data) ? cRes.data : (cRes.data && cRes.data.records ? cRes.data.records : [])
  } catch (e) {
  }
}

function onShare() {
  ElMessage.success('链接已复制（演示）')
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
.dynamic-detail {
  padding: 30px 0 40px;
}

.detail-wrap {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-card {
  padding: 24px 26px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

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
      font-size: 16px;
      font-weight: 700;
      color: #2d2d3a;
      cursor: pointer;
      &:hover { color: #ff4f8b; }
    }
    .time {
      font-size: 12px;
      color: #aaa;
    }
  }
}

.content {
  font-size: 16px;
  line-height: 1.8;
  color: #3a3a4a;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 16px;
}

.image-grid {
  display: grid;
  gap: 6px;
  margin-bottom: 16px;
  border-radius: 10px;
  overflow: hidden;

  &.grid-1 {
    grid-template-columns: 1fr;
    max-width: 480px;
    .img-item { aspect-ratio: 4/3; }
  }
  &.grid-2 {
    grid-template-columns: repeat(2, 1fr);
    .img-item { aspect-ratio: 1; }
  }
  &.grid-3, &.grid-4, &.grid-5, &.grid-6, &.grid-7, &.grid-8, &.grid-9 {
    grid-template-columns: repeat(3, 1fr);
    .img-item { aspect-ratio: 1; }
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
    &:hover img { transform: scale(1.08); }
  }
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid #f3f3f7;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border: none;
  background: transparent;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  color: #7a7a8a;
  transition: all 0.25s ease;

  .icon { font-size: 17px; }

  &:hover {
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
    color: #ff4f8b;
  }
  &.active {
    color: #ff4f8b;
    background: #fff0f5;
    .icon { color: #ff4f8b; }
  }
}

.comment-section {
  padding: 24px 26px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #2d2d3a;
  margin: 0 0 18px;

  .bar {
    width: 5px;
    height: 18px;
    border-radius: 3px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
  }
}

.comment-input {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .mf-btn {
    padding: 9px 20px;
    flex-shrink: 0;
  }
}

.comment-list {
  border-top: 1px solid #f3f3f7;
  padding-top: 6px;
}

.empty-comment {
  text-align: center;
  padding: 30px 0;
  color: #aaa;
  font-size: 14px;
}

.reply-dialog {
  :deep(.el-dialog__header) {
    padding: 18px 24px 10px;
    border-bottom: 1px solid #f3f3f7;
  }
  :deep(.el-dialog__body) {
    padding: 16px 24px 10px;
  }
}

.reply-dialog-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 16px;
  font-weight: 700;
  color: #2d2d3a;

  .reply-target-hint {
    font-size: 13px;
    font-weight: 500;
    color: #6b6b7b;
    b { color: #a855f7; }
  }
}

/* ================== 响应式 ================== */
@media (max-width: 1023px) {
  .dynamic-detail { padding: 20px 0 30px; }
}
@media (max-width: 767px) {
  .dynamic-detail { padding: 12px 0 24px; }
  .detail-card, .comment-section { padding: 14px 14px 16px !important; border-radius: 12px !important; }
  .comment-input { flex-direction: column; align-items: stretch; gap: 8px; margin-bottom: 14px; }
  .comment-input .mf-btn { width: 100%; justify-content: center; }
  .section-title { font-size: 16px; }
}
@media (max-width: 479px) {
  .detail-card, .comment-section { padding: 12px 10px 14px !important; }
}
</style>
