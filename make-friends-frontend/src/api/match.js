import request from './request'

// 喜欢/心动某个用户
export function likeUser(toUserId) {
  return request({
    url: `/match/like/${toUserId}`,
    method: 'post'
  })
}

// 取消喜欢
export function unlikeUser(toUserId) {
  return request({
    url: `/match/unlike/${toUserId}`,
    method: 'delete'
  })
}

// 匹配状态
export function getMatchStatus(userId) {
  return request({
    url: `/match/status/${userId}`,
    method: 'get'
  })
}

// 匹配列表（互相喜欢）
export function getMatchList(params) {
  return request({
    url: '/match/mutual',
    method: 'get',
    params
  })
}

// 喜欢我的列表
export function getLikedMeList(params) {
  return request({
    url: '/match/who-likes-me',
    method: 'get',
    params
  })
}

// 我喜欢的列表
export function getMyLikeList(params) {
  return request({
    url: '/match/my-likes',
    method: 'get',
    params
  })
}

// 一次性获取三类计数（match / myLike / likedMe），用于 Tab 小红数字
export function getMatchCounts() {
  return request({
    url: '/match/counts',
    method: 'get'
  })
}
