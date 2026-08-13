<template>
  <div class="login-page">
    <!-- 装饰性浮动圆形 -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>
    <div class="blob blob-4"></div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="brand">
        <div class="brand-logo">♡</div>
        <h1 class="brand-title gradient-text">MakeFriends</h1>
        <p class="brand-sub">遇见有趣的人，开启心动故事</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="onLogin">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            :prefix-icon="Iphone"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="请输入密码"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon class="pwd-toggle" @click="showPwd = !showPwd">
                <View v-if="showPwd" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <div class="extra-line">
          <el-checkbox v-model="remember">记住我</el-checkbox>
          <a class="forget" @click="ElMessage.info('请联系管理员重置密码')">忘记密码？</a>
        </div>

        <button class="login-btn" :class="{ loading }" @click="onLogin" :disabled="loading">
          <span v-if="!loading">登 录</span>
          <span v-else class="loader"></span>
        </button>
      </el-form>

      <div class="divider"><span>或</span></div>

      <div class="bottom-link">
        还没有账号？<router-link to="/register" class="link">去注册 →</router-link>
      </div>
    </div>

    <p class="copyright">© 2026 MakeFriends · 让每一份心动都有回响</p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Iphone, Lock, View, Hide } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const showPwd = ref(false)
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  phone: '',
  password: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ]
}

async function onLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ phone: form.phone, password: form.password })
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功，欢迎回来！')
    const redirect = route.query.redirect || '/home'
    router.push(redirect)
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9ec4 0%, #c4a8ff 100%);
  overflow: hidden;
  padding: 20px;
}

// 装饰浮动圆形
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.55;
  pointer-events: none;
  animation: floatMove 9s ease-in-out infinite;
}
.blob-1 {
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, #fff 0%, rgba(255, 255, 255, 0) 70%);
  top: 8%;
  left: 10%;
}
.blob-2 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #ffd1e8 0%, rgba(255, 209, 232, 0) 70%);
  bottom: 12%;
  right: 12%;
  animation-delay: 1.5s;
}
.blob-3 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, #e9d5ff 0%, rgba(233, 213, 255, 0) 70%);
  top: 20%;
  right: 22%;
  animation-delay: 3s;
}
.blob-4 {
  width: 90px;
  height: 90px;
  background: radial-gradient(circle, #ffe4f1 0%, rgba(255, 228, 241, 0) 70%);
  bottom: 25%;
  left: 22%;
  animation-delay: 4.5s;
}

.login-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 44px 40px 36px;
  box-shadow: 0 24px 60px rgba(168, 85, 247, 0.28);
  animation: cardIn 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand {
  text-align: center;
  margin-bottom: 30px;

  .brand-logo {
    width: 64px;
    height: 64px;
    margin: 0 auto 12px;
    border-radius: 20px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 34px;
    color: #fff;
    box-shadow: 0 10px 24px rgba(255, 107, 157, 0.45);
    animation: heartBeat 2.5s ease-in-out infinite;
  }

  .brand-title {
    font-size: 28px;
    font-weight: 800;
    letter-spacing: 0.5px;
    margin: 0 0 6px;
  }

  .brand-sub {
    font-size: 14px;
    color: #9a9aaa;
    margin: 0;
  }
}

.extra-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 22px;
  font-size: 13px;

  .forget {
    color: #a855f7;
    cursor: pointer;
    &:hover {
      color: #9333ea;
      text-decoration: underline;
    }
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 22px rgba(255, 107, 157, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover:not(.loading) {
    transform: translateY(-2px);
    box-shadow: 0 12px 28px rgba(168, 85, 247, 0.5);
  }
  &:active:not(.loading) {
    transform: translateY(0);
  }
  &.loading {
    cursor: wait;
  }
}

.loader {
  width: 22px;
  height: 22px;
  border: 2.5px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spinSlow 0.8s linear infinite;
}

.divider {
  position: relative;
  text-align: center;
  margin: 24px 0 18px;
  color: #c0c0cc;
  font-size: 13px;
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    height: 1px;
    background: #eee;
  }
  span {
    position: relative;
    background: #fff;
    padding: 0 14px;
  }
}

.bottom-link {
  text-align: center;
  font-size: 14px;
  color: #8a8a9a;

  .link {
    color: #ff4f8b;
    font-weight: 600;
    &:hover {
      text-decoration: underline;
    }
  }
}

.copyright {
  position: relative;
  z-index: 2;
  margin-top: 28px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.pwd-toggle {
  cursor: pointer;
  color: #aaa;
  &:hover {
    color: #ff4f8b;
  }
}

:deep(.el-input__wrapper) {
  padding: 4px 14px;
  border-radius: 12px !important;
}
:deep(.el-form-item) {
  margin-bottom: 20px;
}

/* ================== LoginView 响应式 ================== */
@media (max-width: 1023px) {
  .login-page { min-height: 100dvh; }
  .bg-blob { opacity: 0.55; }
  .login-box {
    width: min(420px, 92vw);
    padding: 32px 28px;
  }
}
@media (max-width: 767px) {
  .login-box {
    width: 94vw;
    padding: 24px 20px;
    border-radius: 18px;
    margin-top: -20px;
  }
  .logo-wrap .logo-icon { font-size: 40px; }
  .app-title { font-size: 22px; }
  .app-slogan { font-size: 13px; }
  .section-title { font-size: 18px; margin: 16px 0 14px; }
  :deep(.el-form-item) { margin-bottom: 16px; }
  .btn-primary {
    height: 44px; font-size: 14.5px;
  }
  .footer-links {
    flex-direction: column; align-items: center; gap: 8px; margin-top: 12px;
  }
  .copyright { margin-top: 20px; font-size: 11.5px; }
}
@media (max-width: 479px) {
  .login-box {
    width: 100vw;
    height: calc(100dvh - 20px);
    margin: 10px 0 0;
    border-radius: 18px 18px 0 0;
    padding: 20px 18px 30px;
    box-shadow: 0 -8px 24px rgba(255, 107, 157, 0.18);
  }
  .logo-wrap { gap: 6px; }
  .logo-wrap .logo-icon { font-size: 34px; }
  .app-title { font-size: 19px; letter-spacing: 0.2px; }
  .app-slogan { font-size: 12px; }
  .section-title { font-size: 16px; margin: 14px 0 12px; }
}
</style>
