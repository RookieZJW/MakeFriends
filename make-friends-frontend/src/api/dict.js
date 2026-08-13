import request from './request'

// 获取兴趣爱好字典（GET /dict/hobbies）
export function getDictHobbies() {
  return request({
    url: '/dict/hobbies',
    method: 'get'
  })
}

// 获取职业字典（GET /dict/occupations）
export function getDictOccupations() {
  return request({
    url: '/dict/occupations',
    method: 'get'
  })
}

// 一次性拉取全部字典（hobbies + occupations）
export function getDictAll() {
  return request({
    url: '/dict/all',
    method: 'get'
  })
}
