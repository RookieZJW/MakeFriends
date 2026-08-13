# MakeFriends Frontend · 交友平台前端 💚

基于 **Vue 3 + Vite 5 + Element Plus + Pinia + Sass** 构建的交友网站单页应用（SPA）。配套后端为 `make-friends-backend`（Spring Boot 3）。

- 开发地址：http://localhost:5173/
- 接口代理：`/api/*` → `http://localhost:8080/api/*`（Vite dev proxy）
- 打包产物：`dist/`

---

## 🧱 技术选型

| 组件 | 版本 | 说明 |
|---|---|---|
| Vue | 3.4+ | `<script setup>` SFC + Composition API |
| Vite | 5.x | 毫秒级冷启动 + 原生 ESM HMR |
| Element Plus | 2.14+ | ElInput / ElButton / ElForm / ElCard / ElPagination / ElImageViewer / ElTabs |
| Pinia | 2.x | `useUserStore()` 集中管理 token / userInfo / login() / logout() |
| Vue Router | 4.x | 路由 + 全局 `beforeEach` 登录守卫 |
| Axios | 1.7+ | `request.js` 封装：请求注入 satoken / 响应统一 Result.code 判断 / 401 自动跳登录 |
| Sass (dart-sass) | - | `<style lang="scss">` + CSS 变量实现 7 套主题 |
| VueUse（可选） | - | 某些工具函数（如防抖） |

---

## 🚀 启动 & 构建

### 开发模式（热更新）

```powershell
cd make-friends-frontend
npm install          # 首次安装 node_modules（≈600 个包，会慢一点）
npm run dev
# 打开 http://localhost:5173/
```

### 生产构建

```powershell
npm run build
# → dist/ 全部静态资源（index.html + assets/）
```

### 本地预览打包后的产物

```powershell
npm run preview
```

---

## 📁 目录结构

```
make-friends-frontend/
├── index.html                       # 入口 HTML（挂载点 #app）
├── package.json                     # 依赖 + 脚本
├── vite.config.js                   # Vite 配置（server.proxy / alias @ / build）
└── src/
    ├── main.js                      # createApp + Pinia + Router + ElementPlus 注册
    ├── App.vue                      # <RouterView /> + 全局样式
    ├── api/                         # axios 接口层（和后端 Controller 一一对应）
    │   ├── request.js               # axios 实例 + 拦截器
    │   ├── auth.js                  # login / register / logout
    │   ├── user.js                  # me / update / detail / list
    │   ├── match.js                 # like / unlike / status / my-likes / mutual
    │   ├── dynamic.js               # publish / list / detail / user-dynamics
    │   ├── chat.js                  # sessions / messages (page/pageSize) / send / read / unread-count
    │   └── upload.js                # uploadImage(formData)
    ├── assets/styles/
    │   ├── global.scss              # 全局 reset / 字体 / 滚动条
    │   └── variables.scss           # 主题颜色 SCSS 变量（可选，另一种是 CSS 变量）
    ├── components/                  # 通用可复用组件
    │   ├── UserCard.vue             # 首页推荐用户卡片（头像 / 昵称 / 城市 / 爱好标签 / 喜欢按钮）
    │   ├── DynamicCard.vue          # 动态卡片（作者头 + 图片轮播 + 点赞评论计数）
    │   └── CommentItem.vue          # 评论项（支持二级回复缩进 + @用户）
    ├── layouts/
    │   └── MainLayout.vue           # 顶栏（头像下拉）+ 左侧导航 + 中间内容 <RouterView>
    ├── router/
    │   └── index.js                 # 全部路由表 + beforeEach 未登录跳 /login
    ├── stores/
    │   └── user.js                  # Pinia store (token, userInfo, persist = localStorage)
    ├── utils/
    │   └── format.js                # formatTime / genderText / avatarDefault / countText
    └── views/
        ├── auth/
        │   ├── LoginView.vue        # 粉紫渐变背景 + 手机号/密码 + 注册链接
        │   └── RegisterView.vue     # 手机号/昵称/性别/密码 + 表单校验
        ├── home/HomeView.vue        # 筛选条（性别/城市/年龄/爱好/职业）+ 用户卡瀑布
        ├── user/
        │   ├── ProfileView.vue      # 我的主页 + 我的动态 tab
        │   ├── ProfileEditView.vue  # 头像上传 + 表单提交
        │   └── UserHomeView.vue     # 他人主页（看对方动态 + 喜欢按钮）
        ├── dynamic/
        │   ├── DynamicListView.vue  # 广场分页列表 + 发布按钮
        │   ├── DynamicPublishView.vue # ElUpload 多图 + 内容 textarea
        │   └── DynamicDetailView.vue  # 动态 + 评论区（二级回复输入）
        ├── chat/
        │   ├── ChatListView.vue     # 左侧会话列表（未读红点 / 最新消息预览）
        │   └── ChatRoomView.vue     # ★ 聊天室核心：主题 / 表情 / 懒加载 / 右键菜单
        └── match/MatchListView.vue  # Tabs：互关 / 我喜欢的 / 喜欢我的
```

---

## ⚙️ vite.config.js 关键配置

```js
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',    // ← 后端端口
        changeOrigin: true,
        // ws: true  // 如需让 dev server 代理 WebSocket
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
})
```

---

## 💎 特色功能详解（ChatRoomView.vue）

### ① 7 套皮肤主题

- 实现方式：`:root` 上写一套 CSS 变量 `--mf-primary / --mf-bg / --mf-self-bubble / --mf-other-bubble` 等，切换主题时 `applyThemeVars(id)` 通过 `document.documentElement.style.setProperty()` 覆盖
- 持久化：`localStorage['mf_chat_theme']`，下次进入直接恢复
- 主题面板：Teleport 到 body 避免被消息区 overflow 截断

内置主题一览：

| id | 名字 | 主色 |
|---|---|---|
| sakura | 🌸 樱花粉 | #F48FB1 |
| mint | 🍃 薄荷绿 | #81C784 |
| sky | ☁️ 天空蓝 | #64B5F6 |
| starry | 🌌 星夜紫 | #9575CD |
| sun | ☀️ 暖阳橙 | #FFB74D |
| latte | ☕ 奶咖色 | #A1887F |
| pure | ⚪ 极简白 | #9E9E9E |

### ② 分类表情包 + 最近使用

- 数据：`EMOJI_CATEGORIES` 7 组，共约 200+ 个 Unicode emoji
- 最近使用：`loadRecentEmojis()` 从 `localStorage['mf_recent_emojis']` 读（最多 24），每插入一个写回
- 精确光标插入：`insertEmojiAtCursor(emoji)` 通过 `textarea.selectionStart/End` 计算，拼接字符串后再把光标设到插入点之后 + `nextTick(() => textarea.focus())`

### ③ 历史消息分页懒加载（首屏 20 条）

```js
const PAGE_SIZE = 20
const pageNow = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
```

- **loadMessages**：进入时 page=1，拿到最新 20 条 `scrollToBottom()`
- **loadMoreHistory**：`page = pageNow + 1` 拉更早历史；记录加载前 `oldHeight / oldTop`，prepend 完 `newTop = scrollTop + (newHeight - oldHeight)` 保持位置不跳
- **onScroll**：`msgArea.scrollTop <= 30` 时触发；`_scrollLockUntil` 1.2s 防抖防止并发
- **顶部提示**：`loadingMore ? '加载中…' : hasMore ? '⬆ 上滑查看更早消息' : '— 以上就是全部历史消息了 —'`
- **轮询 startPolling**：每 2s 拉取 `pageSize = pageNow × 20` 条同步全量已加载；`wasNearBottom` 判断只有用户接近底部时新消息才 `scrollToBottom`

### ④ 右键消息菜单

消息气泡 `@contextmenu.prevent="onMsgRightClick($event, msg)"`：

- 复制：`navigator.clipboard.writeText(msg.content)`
- 转发：打开一个简易选择会话的弹框（本项目中用 ElMessageBox.prompt 快速实现，可扩展）
- 撤回：自己发的 && `Date.now() - msg.createdAt < 120000` 才显示按钮，调用 `chat.js` 的撤回接口后把气泡改成 `「你撤回了一条消息」` 样式

菜单用 `<Teleport to="body">` 渲染，`position: fixed; left: clientX; top: clientY`，全局 `document.addEventListener('click')` 关掉。

---

## 🛰 API 约定

所有接口**统一前缀 `/api`**，返回体结构：

```json
{ "code": 200, "msg": "ok", "data": {} }
```

分页返回体（聊天消息 / 动态 / 用户列表等）：

```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 128,
    "size": 20,
    "current": 1,
    "pages": 7
  }
}
```

### 鉴权

- 登录成功：`data` 字段直接是 `satoken`（字符串），不是对象
- 存：`localStorage.setItem('satoken', xxx)` → Pinia 同步
- 发：`request.js` 的请求拦截器自动加 `headers.satoken = satoken`
- 过期：响应拦截器 `code === 401` → `StpUtil.logout()` + `router.push('/login')`

---

## 🧪 自测清单

1. ✅ 未登录访问任意路由 → 自动跳 `/login`
2. ✅ 登录成功 → `localStorage.satoken` + `Pinia.userInfo` 都有值 → 跳 `/home`
3. ✅ 首页筛选栏「性别 / 城市 / 爱好 / 职业」→ 请求 `GET /user/list` 返回分页结果
4. ✅ 点用户卡「喜欢」→ 双向喜欢后聊天列表里多一条新会话
5. ✅ 打开聊天室 → 首屏 20 条 → 上滑加载更早 20 条且**当前消息不跳**
6. ✅ 切皮肤 → localStorage 有 → 刷新仍生效
7. ✅ 点表情 → 在光标位置插入 + 最近使用面板出现
8. ✅ 右键自己 2 分钟内的消息 → 可撤回 → 气泡样式变化 + 双方同步
9. ✅ `npm run build` → 成功出 `dist/`，无报错无 warning

---

## 🛠 常见问题

### `npm install` 卡住

→ 换 npmmirror：`npm config set registry https://registry.npmmirror.com`，再试一次。

### 接口全是 404 / Network Error

→ 看 `vite.config.js` 的 `proxy./api.target` 是否指向正在运行的后端；浏览器 DevTools Network 里检查请求 URL 是不是 `http://localhost:5173/api/xxx` 且响应 200。

### 滚动条 / 气泡样式错位

→ 检查 `<style>` 是否加了 `lang="scss"` 且没有加 `scoped`（ChatRoomView 的 `.msg-area` 滚动容器和气泡样式都要求能作用到内部 div）。

### 表情面板 / 主题面板点不出来

→ 按 F12 Elements 搜 `<body>` 底部是否有 Teleport 出来的 `<div class="emoji-panel-wrapper">`（它们不是 `#app` 子元素，所以调试时要找 body 末尾）。面板显隐通过 `showEmojiPanel.value` 控制，点击外部区域自动关闭。

### 懒加载后页面跳

→ 确认 `loadMoreHistory` 里有 `nextTick` 之后再设置 `scrollTop += ΔHeight`；如果数据量很大可以用 `requestAnimationFrame` 再包一层。
