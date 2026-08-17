<template>
  <div class="main-layout">
    <!-- 顶部导航栏（毛玻璃）-->
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="router.push('/home')">
          <span class="logo-mark">搭</span>
          <span class="logo-text">搭伴</span>
        </div>

        <nav class="menu">
          <router-link to="/home" class="menu-item" :class="{ active: isActive('/home') }">
            <el-icon><House /></el-icon><span>首页</span>
          </router-link>
          <router-link to="/dynamic" class="menu-item" :class="{ active: isActive('/dynamic') }">
            <el-icon><PictureRounded /></el-icon><span>动态</span>
          </router-link>
          <router-link to="/chat" class="menu-item" :class="{ active: isActive('/chat') }">
            <el-icon><ChatDotRound /></el-icon>
            <span>消息</span>
            <span class="badge" v-if="unread > 0">{{ unread > 99 ? '99+' : unread }}</span>
          </router-link>
          <router-link to="/match" class="menu-item" :class="{ active: isActive('/match') }">
            <el-icon><Connection /></el-icon><span>匹配</span>
          </router-link>
          <router-link to="/profile" class="menu-item" :class="{ active: isActive('/profile') }">
            <el-icon><User /></el-icon><span>我的</span>
          </router-link>
        </nav>

        <div class="nav-right">
          <button class="pub-btn" @click="router.push('/dynamic/publish')">
            <el-icon><Plus /></el-icon><span>发布</span>
          </button>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="avatar-wrap">
              <el-avatar :size="38" :src="avatarUrl" />
              <span class="nick">{{ userStore.nickname }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>我的主页</el-dropdown-item>
                <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑资料</el-dropdown-item>
                <el-dropdown-item command="match"><el-icon><Connection /></el-icon>我的匹配</el-dropdown-item>
                <el-dropdown-item divided command="logout" style="color:#f43f5e">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="slide-up" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  House, PictureRounded, ChatDotRound, Connection, User,
  ArrowDown, Edit, SwitchButton, Plus
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { resolveAvatar } from '@/utils/format'
import { getUnreadCount } from '@/api/chat'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const unread = ref(0)

const avatarUrl = computed(() => resolveAvatar(userStore.userInfo && userStore.userInfo.avatar, userStore.nickname))

function isActive(path) {
  if (path === '/home') return route.path === '/home' || route.path === '/'
  return route.path.startsWith(path)
}

async function fetchUnread() {
  if (!userStore.isLogin) return
  try {
    const res = await getUnreadCount()
    unread.value = res.data || res || 0
  } catch (e) {
    // 忽略
  }
}

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'edit') router.push('/profile/edit')
  else if (cmd === 'match') router.push('/match')
  else if (cmd === 'logout') doLogout()
}

async function doLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (e) {
    // 取消
  }
}

onMounted(() => {
  if (!userStore.userInfo && userStore.isLogin) {
    userStore.fetchUserInfo()
  }
  fetchUnread()
  // 每 30 秒刷新未读数
  setInterval(fetchUnread, 30000)
})
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;
  position: relative;
  z-index: 1;
  background: transparent;
}

/* ==================== 导航栏 ==================== */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid rgba(91, 141, 239, 0.08);
  transition: box-shadow 0.3s ease;
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* ===== Logo ===== */
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;

  .logo-mark {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    background: linear-gradient(135deg, #5B8DEF 0%, #FFB5C5 100%);
    color: #fff;
    font-size: 16px;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(91, 141, 239, 0.3);
    transition: transform 0.2s ease;
  }

  &:hover .logo-mark {
    transform: scale(1.06) rotate(-3deg);
  }

  .logo-text {
    font-size: 18px;
    font-weight: 800;
    color: #2F3443;
    letter-spacing: 1px;
  }
}

/* ===== 菜单 ===== */
.menu {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 2px;
}

.menu-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #6B7280;
  cursor: pointer;
  transition: all 0.2s ease;

  .el-icon {
    font-size: 17px;
  }

  &:hover {
    color: #2F3443;
    background: rgba(91, 141, 239, 0.06);
  }

  &.active {
    color: #5B8DEF;
    font-weight: 600;
    background: transparent;

    &::after {
      content: '';
      position: absolute;
      bottom: 2px;
      left: 16px;
      right: 16px;
      height: 2px;
      border-radius: 2px;
      background: linear-gradient(90deg, #5B8DEF, #FFB5C5);
    }
  }

  .badge {
    position: absolute;
    top: 2px;
    right: 6px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 999px;
    background: #f43f5e;
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
    font-weight: 700;
    border: 1.5px solid #fff;
  }
}

/* ===== 右侧区域 ===== */
.nav-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.pub-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #5B8DEF 0%, #7FA6F5 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  box-shadow: 0 4px 12px rgba(91, 141, 239, 0.28);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(91, 141, 239, 0.38);
  }

  .el-icon { font-size: 15px; }
}

.avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 3px 10px 3px 3px;
  border-radius: 999px;
  transition: background 0.2s;

  &:hover {
    background: rgba(91, 141, 239, 0.06);
  }

  .nick {
    font-size: 13px;
    font-weight: 600;
    color: #3a3a4a;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .caret {
    font-size: 12px;
    color: #aaa;
  }
}

.content {
  min-height: calc(100vh - 60px);
}

/* ==================== 响应式 ==================== */
@media (max-width: 1023px) {
  .nav-inner {
    max-width: 100%;
    padding: 0 16px;
    gap: 16px;
  }
  .logo .logo-text { font-size: 16px; }
  .menu { gap: 0; }
  .menu-item {
    padding: 8px 12px;
    font-size: 13px;
  }
  .pub-btn { padding: 6px 12px; font-size: 12px; }
  .pub-btn span { display: none; }
  .avatar-wrap .nick { display: none; }
}

@media (max-width: 767px) {
  .nav-inner {
    height: 52px;
    padding: 0 10px;
    gap: 4px;
  }
  .logo { gap: 4px; }
  .logo .logo-mark { width: 28px; height: 28px; font-size: 14px; border-radius: 8px; }
  .logo .logo-text { font-size: 15px; }

  .menu { justify-content: flex-start; overflow-x: auto; scrollbar-width: none; }
  .menu::-webkit-scrollbar { display: none; }
  .menu-item {
    padding: 7px 10px;
    font-size: 0;
    gap: 0;
    .el-icon { font-size: 18px; }
    span:not(.badge) { display: none; }
    .badge { right: 2px; top: 0; }

    &.active::after { display: none; }
    &.active { background: rgba(91, 141, 239, 0.1); border-radius: 8px; }
  }

  .pub-btn {
    padding: 6px 10px;
    .el-icon { font-size: 16px; }
  }
  .avatar-wrap { padding: 3px; }
  .content { min-height: calc(100vh - 52px); }
}

@media (max-width: 479px) {
  .nav-inner { padding: 0 8px; gap: 2px; }
  .logo .logo-text { display: none; }
  .menu-item { padding: 7px 8px; }
  .pub-btn { padding: 5px 8px; }
}
</style>
