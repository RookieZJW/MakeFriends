// 时间格式化工具

/**
 * 格式化时间为相对时间（如：刚刚、3分钟前、2小时前）
 */
export function formatRelativeTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = (now.getTime() - date.getTime()) / 1000

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`
  if (diff < 31536000) return `${Math.floor(diff / 2592000)}个月前`
  return `${Math.floor(diff / 31536000)}年前`
}

/**
 * 格式化为 yyyy-MM-dd HH:mm
 */
export function formatDateTime(time) {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const pad = (n) => (n < 10 ? '0' + n : n)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 格式化为 yyyy-MM-dd
 */
export function formatDate(time) {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''
  const pad = (n) => (n < 10 ? '0' + n : n)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 性别转文字
 */
export function genderText(gender) {
  if (gender === 1 || gender === '1') return '男'
  if (gender === 2 || gender === '2') return '女'
  return '保密'
}

/**
 * 性别对应 emoji
 */
export function genderEmoji(gender) {
  if (gender === 1 || gender === '1') return '♂'
  if (gender === 2 || gender === '2') return '♀'
  return '⚢'
}

/**
 * 默认头像（根据昵称首字符生成）
 */
export function defaultAvatar(name) {
  const text = name ? name.charAt(0) : 'U'
  const colors = ['#ff6b9d', '#a855f7', '#ec4899', '#f43f5e', '#8b5cf6', '#d946ef']
  const color = colors[(name || '').length % colors.length]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="${color}"/><text x="50%" y="50%" dy=".35em" text-anchor="middle" font-family="sans-serif" font-size="56" fill="#fff" font-weight="bold">${text}</text></svg>`
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}

/**
 * 处理头像地址，缺失时用默认头像
 */
export function resolveAvatar(avatar, name) {
  if (avatar && avatar.startsWith('http')) return avatar
  if (avatar && avatar.startsWith('/api')) return avatar
  if (avatar && avatar.startsWith('/files')) return '/api' + avatar
  if (avatar && avatar.startsWith('/')) return '/api' + avatar
  if (avatar) return avatar
  return defaultAvatar(name)
}

/**
 * 处理图片地址
 */
export function resolveImage(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api')) return url
  if (url.startsWith('/files')) return '/api' + url
  if (url.startsWith('/')) return '/api' + url
  return url
}

/**
 * 爱好字符串转数组
 */
export function parseHobbies(hobbies) {
  if (!hobbies) return []
  if (Array.isArray(hobbies)) return hobbies
  return hobbies.split(/[,，、\s]+/).filter(Boolean)
}
