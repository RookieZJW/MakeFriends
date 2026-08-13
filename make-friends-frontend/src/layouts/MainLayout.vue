<template>
  <div class="main-layout">
    <!-- 顶部导航栏（毛玻璃）-->
    <header class="navbar">
      <div class="nav-inner">
        <div class="logo" @click="router.push('/home')">
          <span class="logo-icon">♡</span>
          <span class="logo-text gradient-text">MakeFriends</span>
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
  ArrowDown, Edit, SwitchButton
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
  /* 让 App.vue 的动态背景透出来 */
  background: transparent;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom: 1px solid rgba(255, 107, 157, 0.12);
  box-shadow: 0 2px 12px rgba(168, 85, 247, 0.05);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;

  .logo-icon {
    font-size: 26px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .logo-text {
    font-size: 22px;
    font-weight: 800;
    letter-spacing: 0.3px;
  }
}

.menu {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.menu-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 500;
  color: #6a6a7a;
  cursor: pointer;
  transition: all 0.28s ease;

  .el-icon {
    font-size: 17px;
  }

  &:hover {
    color: #ff4f8b;
    background: linear-gradient(135deg, #fff0f5 0%, #f3e8ff 100%);
  }

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    box-shadow: 0 5px 14px rgba(255, 107, 157, 0.35);
  }

  .badge {
    position: absolute;
    top: 2px;
    right: 6px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 999px;
    background: #f43f5e;
    color: #fff;
    font-size: 11px;
    line-height: 18px;
    text-align: center;
    font-weight: 700;
    border: 1.5px solid #fff;
  }
}

.nav-right {
  flex-shrink: 0;
}

.avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  transition: background 0.25s;
  &:hover {
    background: #fff0f5;
  }
  .nick {
    font-size: 14px;
    font-weight: 600;
    color: #3a3a4a;
    max-width: 90px;
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
  min-height: calc(100vh - 64px);
}

/* =========================================
   MainLayout 响应式（3 级断点）
   ========================================= */

/* 平板/大屏手机：≤1023px */
@media (max-width: 1023px) {
  .nav-inner {
    max-width: 100%;
    padding: 0 16px;
    gap: 18px;
  }
  .logo .logo-text { font-size: 18px; }
  .menu { gap: 2px; justify-content: center; }
  .menu-item {
    padding: 8px 12px;
    font-size: 14px;
  }
}

/* 手机：≤767px（隐藏菜单/昵称文字，只留图标，顶栏变紧凑） */
@media (max-width: 767px) {
  .nav-inner {
    height: 54px;
    padding: 0 10px;
    gap: 6px;
  }
  .logo { gap: 4px; }
  .logo .logo-icon { font-size: 22px; }
  .logo .logo-text { font-size: 16px; }

  .menu { justify-content: flex-start; overflow-x: auto; scrollbar-width: none; }
  .menu::-webkit-scrollbar { display: none; }
  .menu-item {
    padding: 7px 10px;
    font-size: 0;   /* 隐藏文字的 <span>，仅显示 icon */
    gap: 0;
    .el-icon { font-size: 18px; }
    span:not(.badge) { display: none; }
    .badge { right: 2px; top: 0; }
  }

  .avatar-wrap {
    padding: 4px 6px 4px 4px;
    .nick { display: none; }
  }
  .content { min-height: calc(100vh - 54px); }
}

/* 小屏手机：≤479px */
@media (max-width: 479px) {
  .nav-inner {
    height: 50px;
    padding: 0 8px;
    gap: 4px;
  }
  .logo .logo-text {
    display: none;  /* 最窄屏直接隐藏文字，只保留 ♡ logo 图标 */
  }
  .menu-item {
    padding: 7px 8px;
  }
  .content { min-height: calc(100vh - 50px); }
}
</style>
