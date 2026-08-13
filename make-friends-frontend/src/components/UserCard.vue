<template>
  <div class="user-card" @click="goDetail">
    <!-- 顶部渐变条 -->
    <div class="card-top" :class="genderClass"></div>

    <!-- 头像（完整显示，不被裁剪） -->
    <div class="avatar-wrap">
      <el-avatar :size="88" :src="avatarUrl" class="avatar-img">
        <span class="avatar-init">{{ (user.nickname || '?').charAt(0) }}</span>
      </el-avatar>
      <div class="gender-badge" :class="genderClass">
        {{ genderEmoji(user.gender) }}
      </div>
    </div>

    <!-- 信息区 -->
    <div class="info">
      <div class="name-row">
        <span class="nickname">{{ user.nickname || '神秘用户' }}</span>
        <span class="age" :class="genderClass" v-if="user.age">{{ user.age }}岁</span>
      </div>
      <div class="meta-row">
        <span class="meta-item" v-if="user.city">
          <el-icon><Location /></el-icon>
          {{ user.city }}
        </span>
        <span class="meta-item" v-if="user.occupation">
          <el-icon><Briefcase /></el-icon>
          {{ user.occupation }}
        </span>
        <span class="meta-item" v-if="user.height">
          <el-icon><Histogram /></el-icon>
          {{ user.height }}cm
        </span>
      </div>
      <div class="hobby-tags" v-if="parsedHobbies.length">
        <span class="hobby-tag" v-for="h in parsedHobbies.slice(0, 3)" :key="h">{{ h }}</span>
        <span class="hobby-tag more" v-if="parsedHobbies.length > 3">+{{ parsedHobbies.length - 3 }}</span>
      </div>
      <p class="signature">{{ user.signature || '这个人很懒，还没有填写简介' }}</p>
    </div>

    <!-- 喜欢按钮 -->
    <div class="actions">
      <button
        class="like-btn"
        :class="{ liked: liked }"
        @click.stop="onLike"
      >
        <span class="heart">{{ liked ? '♥' : '♡' }}</span>
        <span>{{ liked ? '已心动' : '心动' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Location, Briefcase, Histogram } from '@element-plus/icons-vue'
import { genderEmoji, resolveAvatar, parseHobbies } from '@/utils/format'
import { likeUser, unlikeUser } from '@/api/match'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isLiked, addLiked, removeLiked } from '@/utils/likeCache'

const props = defineProps({ user: { type: Object, required: true } })
const emit = defineEmits(['liked', 'unliked'])
const router = useRouter()
const userStore = useUserStore()

/**
 * 注意：不用 ref(!!props.user.liked) 了 —— 那种写法只在组件创建时读一次，
 * 父组件回填 liked 或者缓存更新都不会重新生效。
 * 改为 computed 双读：优先后端/父组件回填，其次本地缓存兜底。
 */
const liked = computed(() => {
  const backendValue = props.user && (props.user.liked === true || props.user.isLiked === true || props.user.likedByMe === true)
  if (backendValue) return true
  return isLiked(props.user && props.user.id, userStore.userId)
})
const avatarUrl = computed(() => resolveAvatar(props.user.avatar, props.user.nickname))

const genderClass = computed(() => {
  const g = props.user.gender
  if (g === 1) return 'male'
  if (g === 2) return 'female'
  return 'secret'
})

const parsedHobbies = computed(() => parseHobbies(props.user.hobbies))

function goDetail() {
  router.push(`/user/${props.user.id}`)
}

async function onLike() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (liked.value) {
      await unlikeUser(props.user.id)
      removeLiked(props.user.id, userStore.userId)
      // 同步回写父组件，保证列表 UI 立刻刷新（如果父组件监听 emit）
      if (props.user) props.user.liked = false
      emit('unliked', props.user.id)
      ElMessage.success('已取消心动')
    } else {
      await likeUser(props.user.id)
      addLiked(props.user.id, userStore.userId)
      if (props.user) props.user.liked = true
      emit('liked', props.user.id)
      ElMessage.success('心动成功')
    }
  } catch (e) { /* handled */ }
}
</script>

<style lang="scss" scoped>
.user-card {
  cursor: pointer;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(91, 141, 239, 0.08);
  border: 1px solid #E5E9F2;
  overflow: hidden;
  position: relative;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.35s ease,
              border-color 0.3s ease;
  display: flex;
  flex-direction: column;

  // 科技感渐变描边（hover 显现）
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    padding: 1px;
    border-radius: 16px;
    background: linear-gradient(135deg, rgba(91, 141, 239, 0.0) 0%, rgba(91, 141, 239, 0.0) 100%);
    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
            mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
            mask-composite: exclude;
    pointer-events: none;
    transition: background 0.4s ease;
    z-index: 3;
  }

  // 扫光（Shimmer）
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -120%;
    width: 65%;
    height: 100%;
    background: linear-gradient(
      100deg,
      transparent 0%,
      rgba(255, 255, 255, 0.65) 50%,
      transparent 100%
    );
    pointer-events: none;
    transition: left 0.85s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 2;
  }

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 32px rgba(91, 141, 239, 0.15);
    border-color: transparent;

    &::before {
      background: linear-gradient(135deg, rgba(91, 141, 239, 0.45) 0%, rgba(255, 181, 197, 0.45) 100%);
    }
    &::after { left: 150%; }
  }
}

.card-top {
  height: 64px;
  border-radius: 16px 16px 0 0;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: -50%; right: -30%;
    width: 140px; height: 140px;
    background: radial-gradient(circle, rgba(255,255,255,0.6) 0%, transparent 70%);
    animation: float-orbs 6s ease-in-out infinite;
  }

  &.male {
    background: linear-gradient(135deg, #A8BEF6 0%, #D4DEF9 100%);
  }
  &.female {
    background: linear-gradient(135deg, #FFC9DE 0%, #FFE5EC 100%);
  }
  &.secret {
    background: linear-gradient(135deg, #D1D5DB 0%, #E5E7EB 100%);
  }
}

@keyframes float-orbs {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(-15px, 10px); }
}

.avatar-wrap {
  position: relative;
  margin-top: -44px;
  display: flex;
  justify-content: center;
  z-index: 2;
}

.avatar-img {
  border: 4px solid #fff;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08),
              0 0 0 1px rgba(91, 141, 239, 0.08);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.35s ease;
}

.user-card:hover .avatar-img {
  transform: scale(1.05);
  box-shadow: 0 8px 22px rgba(91, 141, 239, 0.22),
              0 0 0 1px rgba(91, 141, 239, 0.2);
}

.avatar-init {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 36px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #5B8DEF, #89A9F3);
  border-radius: 50%;
}

.gender-badge {
  position: absolute;
  bottom: 2px;
  right: 50%;
  transform: translateX(46px);
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  border: 2px solid #fff;

  &.male { background: #5B8DEF; }
  &.female { background: #F08DA5; }
  &.secret { background: #9CA3AF; }
}

.info {
  padding: 16px 16px 12px;
  text-align: center;
}

.name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;

  .nickname {
    font-size: 16px;
    font-weight: 600;
    color: #2F3443;
  }

  .age {
    font-size: 12px;
    padding: 2px 10px;
    border-radius: 999px;
    color: #fff;
    font-weight: 500;

    &.male { background: #5B8DEF; }
    &.female { background: #F08DA5; }
    &.secret { background: #9CA3AF; }
  }
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #6B7280;

    .el-icon { font-size: 12px; color: #89A9F3; }
  }
}

.hobby-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 8px;

  .hobby-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
    background: linear-gradient(135deg, #F4F7FE 0%, #F0E6FF 100%);
    color: #8b5cf6;
    font-weight: 500;
    white-space: nowrap;

    &.more {
      background: #F3F4F6;
      color: #6B7280;
    }
  }
}

.signature {
  margin: 0;
  font-size: 13px;
  color: #6B7280;
  line-height: 1.5;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.actions {
  padding: 0 16px 16px;
}

.like-btn {
  width: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 0;
  border: 1px solid #E5E9F2;
  border-radius: 12px;
  background: #fff;
  color: #5B8DEF;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;

  // 扫光
  &::after {
    content: '';
    position: absolute;
    top: 0; left: -120%;
    width: 60%;
    height: 100%;
    background: linear-gradient(100deg, transparent 0%, rgba(255,255,255,0.6) 50%, transparent 100%);
    transition: left 0.7s ease;
  }
  &:hover::after { left: 150%; }

  .heart { font-size: 15px; line-height: 1; transition: transform 0.2s ease; }

  &:hover {
    border-color: #5B8DEF;
    background: #F4F7FE;
    box-shadow: 0 4px 14px rgba(91, 141, 239, 0.15);

    .heart { transform: scale(1.2); }
  }

  &.liked {
    background: linear-gradient(135deg, #F08DA5 0%, #FFB5C5 100%);
    border-color: transparent;
    color: #fff;
    box-shadow: 0 4px 14px rgba(240, 141, 165, 0.35);

    &::after {
      background: linear-gradient(100deg, transparent 0%, rgba(255,255,255,0.7) 50%, transparent 100%);
    }

    .heart { animation: heartBeat 0.6s ease; }
  }
}
</style>