import request from './request'

// 动态广场列表
export function getDynamicList(params) {
  return request({
    url: '/dynamic/list',
    method: 'get',
    params
  })
}

// 我的动态
export function getMyDynamics(params) {
  return request({
    url: '/dynamic/my',
    method: 'get',
    params
  })
}

// 某个用户的动态
export function getUserDynamic(userId, params) {
  return request({
    url: `/dynamic/user/${userId}`,
    method: 'get',
    params
  })
}

// 动态详情
export function getDynamicDetail(id) {
  return request({
    url: `/dynamic/${id}`,
    method: 'get'
  })
}

// 发布动态
export function publishDynamic(data) {
  return request({
    url: '/dynamic',
    method: 'post',
    data
  })
}

// 删除动态
export function deleteDynamic(id) {
  return request({
    url: `/dynamic/${id}`,
    method: 'delete'
  })
}

// 点赞/取消点赞（后端 LikeController）
export function toggleLike(id) {
  return request({
    url: `/like/toggle/${id}`,
    method: 'post'
  })
}

// 检查是否已赞
export function checkLiked(id) {
  return request({
    url: `/like/check/${id}`,
    method: 'get'
  })
}

// 评论列表（后端 CommentController）
export function getComments(dynamicId, params) {
  return request({
    url: `/comment/list/${dynamicId}`,
    method: 'get',
    params
  })
}

// 发表评论
export function addComment(data) {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

// 删除评论
export function deleteComment(id) {
  return request({
    url: `/comment/${id}`,
    method: 'delete'
  })
}

// 上传动态图片（复用通用上传接口）
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
