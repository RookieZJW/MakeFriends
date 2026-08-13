import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    userId: (state) => state.userInfo && state.userInfo.id,
    nickname: (state) => (state.userInfo && state.userInfo.nickname) || '游客',
    avatar: (state) => (state.userInfo && state.userInfo.avatar) || ''
  },

  actions: {
    // 登录
    async login(form) {
      const res = await loginApi(form)
      // 兼容多种返回结构：res.data.token / res.data / res.token
      const token = (res.data && res.data.token) || res.data || res.token
      if (token) {
        this.token = typeof token === 'string' ? token : token.tokenValue || String(token)
        localStorage.setItem('token', this.token)
      }
      // 若返回中带用户信息则一并存储
      if (res.data && res.data.userInfo) {
        this.userInfo = res.data.userInfo
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      }
      return res
    },

    // 获取用户信息
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res.data || res
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        return this.userInfo
      } catch (e) {
        return null
      }
    },

    // 退出登录
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 忽略退出接口错误
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },

    // 本地更新用户信息
    setUserInfo(info) {
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    }
  }
})
