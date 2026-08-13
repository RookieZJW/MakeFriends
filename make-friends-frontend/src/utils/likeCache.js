/**
 * 心动状态本地缓存（localStorage 持久化兜底）
 *
 * 场景：推荐列表接口 getRecommendUsers 如果后端没返回「liked / isLiked」字段，
 * 前端刷新后 props.user.liked 就会回到 undefined。
 * 这里统一维护 "当前登录用户 → 点过心动的目标用户 ID Set"，保证刷新不丢失。
 *
 * ID 全部强制 String 化，避免 number / string 混用导致 includes 判断失败。
 */

const PREFIX = 'mf_liked'

function cacheKey(currentUserId) {
  const uid = String(currentUserId || 'guest')
  return `${PREFIX}:${uid}`
}

function readSet(currentUserId) {
  try {
    const raw = localStorage.getItem(cacheKey(currentUserId))
    if (!raw) return new Set()
    const arr = JSON.parse(raw)
    return new Set(Array.isArray(arr) ? arr.map(String) : [])
  } catch (e) {
    return new Set()
  }
}

function writeSet(currentUserId, set) {
  try {
    const arr = Array.from(set)
    localStorage.setItem(cacheKey(currentUserId), JSON.stringify(arr))
  } catch (e) {
    /* quota 满 / 隐身模式：忽略 */
  }
}

/** 是否已心动（带 userId 上下文） */
export function isLiked(targetId, currentUserId) {
  if (targetId == null) return false
  const set = readSet(currentUserId)
  return set.has(String(targetId))
}

/** 写入心动 */
export function addLiked(targetId, currentUserId) {
  if (targetId == null) return
  const set = readSet(currentUserId)
  set.add(String(targetId))
  writeSet(currentUserId, set)
}

/** 取消心动 */
export function removeLiked(targetId, currentUserId) {
  if (targetId == null) return
  const set = readSet(currentUserId)
  set.delete(String(targetId))
  writeSet(currentUserId, set)
}

/**
 * 把本地缓存的 liked 状态批量回填到 user 列表上。
 * 每个 user.liked = 后端原值 || 本地缓存值（优先后端返回的真值）
 */
export function applyLikedToUsers(users, currentUserId) {
  if (!Array.isArray(users)) return users
  const set = readSet(currentUserId)
  for (const u of users) {
    if (!u || u.id == null) continue
    const id = String(u.id)
    // 后端如果已经返回了 liked/isLiked，以后端为准；否则用本地缓存
    const backendLiked = u.liked === true || u.isLiked === true || u.likedByMe === true
    u.liked = backendLiked || set.has(id)
  }
  return users
}
