<template>
  <!-- 全局动态科技感背景（放在最顶层，所有页面都会显示） -->
  <div class="app-bg" aria-hidden="true">
    <div class="app-bg__grid"></div>
    <div class="app-bg__blob app-bg__blob--blue1"></div>
    <div class="app-bg__blob app-bg__blob--pink1"></div>
    <div class="app-bg__blob app-bg__blob--blue2"></div>
    <div class="app-bg__blob app-bg__blob--pink2"></div>
    <div class="app-bg__noise"></div>
  </div>

  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/stores/user'
import { heartbeat } from '@/api/user'

let timer = null

async function tickHeartbeat() {
  const userStore = useUserStore()
  if (!userStore.token) return
  try {
    await heartbeat()
  } catch (e) {
    // 静默
  }
}

function onWindowClose() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    // 使用同步 XHR 保证关闭前能发出去（通知后端把我置为离线）
    const xhr = new XMLHttpRequest()
    xhr.open('POST', '/api/auth/logout', false)
    xhr.setRequestHeader('satoken', token)
    xhr.setRequestHeader('Content-Type', 'application/json')
    xhr.send()
  } catch (e) {
    // 静默
  }
}

onMounted(() => {
  // 启动时立刻心跳一次
  tickHeartbeat()
  // 30 秒一次（心跳超时判定=60 秒，留余量）
  timer = setInterval(tickHeartbeat, 30 * 1000)
  // 关闭 / 刷新 / 切到后台 都通知离线
  window.addEventListener('beforeunload', onWindowClose)
  window.addEventListener('pagehide', onWindowClose)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  timer = null
  window.removeEventListener('beforeunload', onWindowClose)
  window.removeEventListener('pagehide', onWindowClose)
})
</script>

<style lang="scss">
#app {
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

/* ========== 全局动态科技感背景（放在 #app 之前，所有页面可见） ========== */
.app-bg {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(180deg, #F7F9FC 0%, #EEF3FB 40%, #FBF1F5 100%);

  /* 科技网格线 */
  &__grid {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(91, 141, 239, 0.08) 1px, transparent 1px),
      linear-gradient(90deg, rgba(91, 141, 239, 0.08) 1px, transparent 1px);
    background-size: 56px 56px;
    mask-image: radial-gradient(ellipse at 50% 30%, #000 0%, #000 35%, transparent 80%);
    -webkit-mask-image: radial-gradient(ellipse at 50% 30%, #000 0%, #000 35%, transparent 80%);
  }

  /* 4 个流动极光斑 */
  &__blob {
    position: absolute;
    width: 480px;
    height: 480px;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.55;
    animation: app-blob-drift 22s ease-in-out infinite;
    will-change: transform;

    &--blue1 {
      left: -120px;
      top: -120px;
      background: radial-gradient(circle, #7FA6F5 0%, #5B8DEF 45%, transparent 70%);
      animation-duration: 28s;
    }
    &--pink1 {
      right: -80px;
      top: 10%;
      background: radial-gradient(circle, #FFCAD9 0%, #FFB5C5 45%, transparent 70%);
      animation-duration: 32s;
      animation-direction: reverse;
      animation-delay: -6s;
    }
    &--blue2 {
      right: -120px;
      bottom: -180px;
      background: radial-gradient(circle, #92B4F7 0%, #7299F0 45%, transparent 70%);
      animation-duration: 26s;
      animation-delay: -14s;
    }
    &--pink2 {
      left: -80px;
      bottom: -120px;
      background: radial-gradient(circle, #FFC9D9 0%, #F5A4B8 45%, transparent 70%);
      animation-duration: 30s;
      animation-direction: reverse;
      animation-delay: -4s;
    }
  }

  /* 颗粒噪点：增加质感 */
  &__noise {
    position: absolute;
    inset: 0;
    opacity: 0.05;
    mix-blend-mode: multiply;
    background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/></filter><rect width='100%' height='100%' filter='url(%23n)' opacity='0.7'/></svg>");
  }
}

@keyframes app-blob-drift {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(60px, 50px) scale(1.06); }
  66%  { transform: translate(-40px, -30px) scale(0.96); }
  100% { transform: translate(0, 0) scale(1); }
}
</style>
