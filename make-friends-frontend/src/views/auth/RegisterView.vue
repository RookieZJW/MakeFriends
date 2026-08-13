<template>
  <div class="register-page">
    <!-- 装饰性浮动圆形 -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>
    <div class="blob blob-4"></div>

    <!-- 注册卡片 -->
    <div class="register-card">
      <div class="brand">
        <div class="brand-logo">♡</div>
        <h1 class="brand-title gradient-text">加入 MakeFriends</h1>
        <p class="brand-sub">创建账号，开启你的缘分之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
        <el-form-item prop="phone" label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Iphone" maxlength="11" />
        </el-form-item>

        <el-form-item prop="nickname" label="昵称">
          <el-input v-model="form.nickname" placeholder="给自己起个昵称" :prefix-icon="User" maxlength="20" />
        </el-form-item>

        <el-form-item prop="gender" label="性别">
          <el-radio-group v-model="form.gender" class="gender-group">
            <el-radio :value="2">
              <span class="gender-opt female">♀ 小仙女</span>
            </el-radio>
            <el-radio :value="1">
              <span class="gender-opt male">♂ 小哥哥</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="password" label="密码">
          <el-input
            v-model="form.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="6-20位密码"
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

        <el-form-item prop="confirmPassword" label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            :type="showPwd2 ? 'text' : 'password'"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon class="pwd-toggle" @click="showPwd2 = !showPwd2">
                <View v-if="showPwd2" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="agree">
          <el-checkbox v-model="form.agree">
            我已阅读并同意 <a class="protocol" @click="ElMessage.info('演示协议')">《用户协议》</a> 和 <a class="protocol" @click="ElMessage.info('演示政策')">《隐私政策》</a>
          </el-checkbox>
        </el-form-item>

        <button class="register-btn" :class="{ loading }" @click="onRegister" :disabled="loading">
          <span v-if="!loading">注 册</span>
          <span v-else class="loader"></span>
        </button>
      </el-form>

      <div class="bottom-link">
        已有账号？<router-link to="/login" class="link">去登录 →</router-link>
      </div>
    </div>

    <p class="copyright">© 2026 MakeFriends · 让每一份心动都有回响</p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Iphone, Lock, View, Hide, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const showPwd = ref(false)
const showPwd2 = ref(false)
const loading = ref(false)

const form = reactive({
  phone: '',
  nickname: '',
  gender: 2,
  password: '',
  confirmPassword: '',
  agree: false
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateAgree = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请先同意用户协议'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度 2-20 位', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  agree: [{ validator: validateAgree, trigger: 'change' }]
}

async function onRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await register({
      phone: form.phone,
      password: form.password,
      nickname: form.nickname,
      gender: form.gender
    })
    ElMessage.success('注册成功，快去登录吧！')
    setTimeout(() => router.push('/login'), 800)
  } catch (e) {
    // 忽略
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #c4a8ff 0%, #ff9ec4 100%);
  overflow: hidden;
  padding: 30px 20px;
}

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
  top: 6%;
  right: 8%;
}
.blob-2 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #ffd1e8 0%, rgba(255, 209, 232, 0) 70%);
  bottom: 10%;
  left: 10%;
  animation-delay: 1.5s;
}
.blob-3 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, #e9d5ff 0%, rgba(233, 213, 255, 0) 70%);
  top: 30%;
  left: 22%;
  animation-delay: 3s;
}
.blob-4 {
  width: 90px;
  height: 90px;
  background: radial-gradient(circle, #ffe4f1 0%, rgba(255, 228, 241, 0) 70%);
  bottom: 28%;
  right: 24%;
  animation-delay: 4.5s;
}

.register-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 440px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px 40px 32px;
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
  margin-bottom: 24px;

  .brand-logo {
    width: 60px;
    height: 60px;
    margin: 0 auto 10px;
    border-radius: 18px;
    background: linear-gradient(135deg, #ff6b9d 0%, #a855f7 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30px;
    color: #fff;
    box-shadow: 0 10px 24px rgba(255, 107, 157, 0.45);
  }

  .brand-title {
    font-size: 26px;
    font-weight: 800;
    margin: 0 0 5px;
  }

  .brand-sub {
    font-size: 14px;
    color: #9a9aaa;
    margin: 0;
  }
}

.gender-group {
  display: flex;
  gap: 20px;

  .gender-opt {
    font-weight: 600;
    &.female {
      color: #ec4899;
    }
    &.male {
      color: #3b82f6;
    }
  }
}

.protocol {
  color: #a855f7;
  &:hover {
    text-decoration: underline;
  }
}

.register-btn {
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
  margin-top: 8px;

  &:hover:not(.loading) {
    transform: translateY(-2px);
    box-shadow: 0 12px 28px rgba(168, 85, 247, 0.5);
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

.bottom-link {
  text-align: center;
  margin-top: 20px;
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
  margin-top: 24px;
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
  margin-bottom: 18px;
}
:deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-weight: 600;
  color: #4a4a5a;
  font-size: 14px;
}

/* ================== RegisterView 响应式 ================== */
@media (max-width: 1023px) {
  .register-page { min-height: 100dvh; }
  .bg-blob { opacity: 0.55; }
  .register-box {
    width: min(480px, 92vw);
    padding: 30px 28px;
  }
  .register-grid { grid-template-columns: 1fr !important; gap: 0 !important; }
}
@media (max-width: 767px) {
  .register-box {
    width: 94vw;
    padding: 22px 18px;
    border-radius: 18px;
  }
  .logo-wrap .logo-icon { font-size: 38px; }
  .app-title { font-size: 21px; }
  .app-slogan { font-size: 12.5px; }
  .section-title { font-size: 17px; margin: 14px 0 12px; }
  :deep(.el-form-item) { margin-bottom: 14px; }
  :deep(.el-form-item__label) { font-size: 13px; }
  .btn-primary { height: 44px; font-size: 14.5px; }
  .footer-links { flex-direction: column; align-items: center; gap: 8px; margin-top: 12px; }
  .copyright { margin-top: 18px; font-size: 11.5px; }
}
@media (max-width: 479px) {
  .register-box {
    width: 100vw;
    margin: 10px 0 0;
    border-radius: 18px 18px 0 0;
    padding: 18px 16px 24px;
    box-shadow: 0 -8px 24px rgba(168, 85, 247, 0.18);
  }
  .logo-wrap { gap: 6px; }
  .logo-wrap .logo-icon { font-size: 32px; }
  .app-title { font-size: 18px; }
  .app-slogan { font-size: 12px; }
  .section-title { font-size: 16px; }
  .avatar-select { gap: 10px; }
  .avatar-opt { width: 50px; height: 50px; --opt-size: 46px; }
}
</style>
