import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：携带 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 如果是文件流等非 JSON 直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === undefined) {
      // 非标准返回，直接返回 data
      return res
    }
    if (res.code !== 200) {
      ElMessage.error(res.msg || res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || res.message || 'Error'))
    }
    return res
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (status >= 500) {
      ElMessage.error('服务器开小差了，请稍后再试')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
