import request from './request'

// 获取当前登录用户信息
export function getUserInfo() {
  return request({
    url: '/user/me',
    method: 'get'
  })
}

// 根据ID获取用户信息
export function getUserById(id) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

// 更新用户资料
export function updateUser(data) {
  return request({
    url: '/user/me',
    method: 'put',
    data
  })
}

// 上传头像（复用通用上传接口）
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 推荐用户列表（后端用 /user/list，支持 gender/city/minAge/maxAge 筛选）
export function getRecommendUsers(params) {
  return request({
    url: '/user/list',
    method: 'get',
    params
  })
}

// 搜索用户（后端没有独立搜索接口，复用 /user/list 带参数）
export function searchUsers(params) {
  return request({
    url: '/user/list',
    method: 'get',
    params
  })
}

// 心跳：标记仍然在线（30s 调一次）
export function heartbeat() {
  return request({
    url: '/user/heartbeat',
    method: 'post'
  })
}

// 设置在线状态：1=在线 2=隐身
export function setOnlineStatus(status) {
  return request({
    url: '/user/online-status',
    method: 'post',
    params: { status }
  })
}
