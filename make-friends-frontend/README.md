# 搭伴 Frontend · 校园互动平台前端 💚

基于 **Vue 3 + Vite 5 + Element Plus + Pinia + Sass** 构建的校园青年互动社区单页应用（SPA）。品牌名「搭伴」，粉蓝（#5B8DEF → #FFB5C5）主色调，轻盈不浮夸，动态特效科技感适中。配套后端：`make-friends-backend`（Spring Boot 3 + Redis + Resilience4j）。

> Slogan：**校园好搭伴，生活不孤单**
> 线上演示：**https://rokbj.me**

- 开发地址：http://localhost:5173/
- 接口代理：`/api/*` → `http://localhost:8080/api/*`（Vite dev proxy）
- 打包产物：`dist/`
- 生产 host 放行：`vite.config.js` 的 `server.allowedHosts = true`（配合 ngrok / Cloudflare Tunnel 直接穿透）

---

## 🧱 技术选型

| 组件 | 版本 | 说明 |
|---|---|---|
| Vue | 3.4+ | `<script setup>` SFC + Composition API |
| Vite | 5.x | 毫秒级冷启动 + 原生 ESM HMR；allowedHosts=true 穿透免 Host 校验 |
| Element Plus | 2.14+ | ElInput / ElButton / ElForm / ElCard / ElPagination / ElImageViewer / ElTabs / ElMessageBox 等 |
| Pinia | 2.x | `useUserStore()` 集中管理 satoken / userInfo / login / logout，persist=localStorage |
| Vue Router | 4.x | 路由 + 全局 `beforeEach` 登录守卫 + document.title 动态切换（「搭伴」） |
| Axios | 1.7+ | `api/request.js` 封装：请求注入 satoken / 响应统一 `Result.code` 判断 / 401 自动跳登录 |
| Sass (dart-sass) | - | `<style lang="scss">` + CSS 变量实现 7 套主题；clamp() 响应式尺寸 |
| Teleport | Vue 内置 | 主题面板 / 表情面板 / 右键消息菜单 → 挂载到 body，避免父级 overflow / backdrop-filter 创建新包含块导致定位异常 |

---

## 🎨 品牌视觉 & UI 规范

**品牌名**：搭伴（原 MakeFriends）
- 理由：不止是交友。预留二手交易、拼单搭饭、搭自习、校园日常等扩展空间。
- Logo：几何胶囊图标「搭」，粉蓝渐变方块（#5B8DEF → #FFB5C5）+ 文字「搭伴」。
- Slogan：校园好搭伴，生活不孤单（登录页）、加入搭伴·开启你的校园搭伴之旅（注册页）。

**导航栏 2026**（MainLayout）：
- 高度：60px（桌面），更紧凑。
- 背景：`.glass-card` 毛玻璃（blur 20px + saturate 180%），细蓝底边 `rgba(91,141,239,.08)`。
- 菜单项：**轻盈 hover 灰底 + active 粉蓝渐变下划线**（不是旧版厚重渐变胶囊）。
- 右侧：「＋发布」快捷按钮，粉蓝渐变方块字，hover 上浮。
- 响应式：
  - 平板 ≤1023px：隐藏昵称，发布按钮只保留渐变图标。
  - 手机 ≤767px：菜单项切换为纯图标，active 改为浅蓝底色（下划线过小屏视觉差）。
  - 极限小屏 ≤479px：隐藏 Logo 文字，只留胶囊几何图标。

**Home Hero 右侧头像**：
- 原装饰渐变圆 → 替换为当前登录用户头像。
- 粉蓝渐变光晕 `pulse-glow` 6 秒呼吸动画，外圈白 4px 描边 + 蓝色投影。
- 兜底：无头像时 → 昵称首字母（自动取第一个字/字母大写）+ 粉蓝渐变圆。
- 断点尺寸：桌面 `inset:26px`，平板 `inset:20px`，手机 `inset:16px`；兜底字体同步缩小（44→34→26px）。

**全局响应式策略**：
| 断点分类 | 宽度 | 处理 |
|---|---|---|
| 桌面 | ≥1024px | 双栏/三栏完整；聊天栏 max-width 跟随 CSS 变量 + clamp()，两侧留白适中 |
| 平板 | 768–1023px | MainLayout 导航降图标密度；个人主页横排降边距；网格列数降级 |
| 手机 | 480–767px | 聊天室隐藏左右栏改为单列；首页用户网格列数 2→1；登录贴底满屏卡片；profile-inner 改为竖排 flex-direction:column 使按钮不被挤出 |
| 极限 | ≤479px | 全局字号缩小；去掉非必要装饰 |

高度断点（聊天页）：≥900px / <720px / <640px → 聊天消息区 min-height 调整，避免尾部挤出视口。

> 聊天栏布局：**Grid 列分配中间栏封顶 + 整体居中**，不写死宽度；内层组件 max-width 随 CSS 变量 + clamp() 响应式。侧边栏**双容器**分离装饰和内容，内层 `overflow-y:auto` 独立滚动；内容卡片强制 `flex:0 0 auto` 避免被压缩，关键卡片 `min-height` 保证完整。

---

## 🚀 启动 & 构建

### 开发模式（热更新）

```powershell
cd make-friends-frontend
npm install          # 首次安装（≈600 个包）
npm run dev
# → http://localhost:5173/
```

### 生产构建

```powershell
npm run build
# → dist/ 全静态资源（index.html + assets/）
```

### 本地预览打包后产物

```powershell
npm run preview
```

### 外网穿透预览（ngrok / Cloudflare Tunnel）

`vite.config.js` 已设置 `server.allowedHosts = true`，不会再报 "This host xxx is not allowed"。直接开隧道：

```powershell
# 任选其一
ngrok http 5173
cloudflared tunnel --url http://localhost:5173
```

---

## 📁 目录结构

```
make-friends-frontend/
├── index.html                           # 入口：title = 搭伴 · 校园好搭伴，生活不孤单
├── package.json                         # 依赖 + 脚本
├── vite.config.js                       # Vite：server.proxy / alias @ / build / allowedHosts=true
└── src/
    ├── main.js                          # createApp + Pinia + Router + ElementPlus（完整引入，按需可改自动导入）
    ├── App.vue                          # <RouterView /> + 全局 reset / 字体 / 滚动条 / 响应式 viewport + 安全区内边距
    ├── api/                             # axios 接口层（与后端 Controller 一一对应）
    │   ├── request.js                   # axios 实例：baseURL=/api, timeout=30s, 请求拦截 inject satoken, 响应拦截 Result.code + 401
    │   ├── auth.js                      # register / login / logout
    │   ├── user.js                      # me / update / detail / list / getUserById（他人主页 + 默契度爱好列表）
    │   ├── match.js                     # like / unlike / status / my-likes / who-likes-me / mutual / tacit默契度
    │   ├── dynamic.js                   # publish / list / detail / user-dynamics / like toggle
    │   ├── comment.js                   # publish / delete / list-by-dynamic
    │   ├── chat.js                      # sessions / messages(page,pageSize) / send / read / unread-count / delete-session
    │   ├── dict.js                      # hobby / occupation 字典（按分类分组）
    │   └── upload.js                    # uploadImage(formData)
    ├── assets/
    │   ├── styles/global.scss           # 全局 reset/字体/滚动条/响应式 6 个公用显示类(.hide-mobile/.hide-tablet/...)
    │   └── hero.png / vite.svg / vue.svg
    ├── components/                      # 通用可复用组件
    │   ├── UserCard.vue                 # 首页推荐卡片：头像/昵称/城市/年龄/爱好标签/喜欢按钮/默契环
    │   ├── DynamicCard.vue              # 动态卡片：作者头+图片多图轮播+点赞评论计数+点进详情
    │   └── CommentItem.vue              # 评论：两级渲染；一级渲染回复列表；二级不再渲染子节点避免无限递归
    ├── layouts/
    │   └── MainLayout.vue               # 顶栏导航（2026新版轻盈粉蓝）+ 主区域 grid 三栏
    ├── router/
    │   └── index.js                     # 路由表 + beforeEach 登录守卫未登录→/login；document.title = 搭伴
    ├── stores/
    │   └── user.js                      # Pinia store (satoken, userInfo)；persist = localStorage
    ├── utils/
    │   ├── format.js                    # formatTime / genderText / avatarDefault / countText
    │   └── likeCache.js                 # 点赞本地缓存，避免闪回
    └── views/
        ├── auth/
        │   ├── LoginView.vue            # 粉紫渐变背景→标题「搭伴」+ slogan + 手机号/密码/登录按钮
        │   └── RegisterView.vue         # 标题「加入搭伴」+ 手机号/昵称/性别/密码 + 表单校验
        ├── home/HomeView.vue            # 筛选条 + 用户卡网格；Hero 右侧当前用户头像光晕浮层
        ├── user/
        │   ├── ProfileView.vue          # 我的主页 + 我的动态 tab；手机端 profile-inner → flex-direction:column 竖排
        │   ├── EditProfileView.vue      # 头像上传 + 资料表单；100+ 爱好配专属 emoji；无爱好时友好空状态
        │   └── UserDetailView.vue       # TA的主页：loadPeerInfo() 取真实爱好列表；三维默契度环形(≥90💘超合拍 等 5 档)
        ├── dynamic/
        │   ├── DynamicSquare.vue        # 广场分页列表 + 右上发布按钮
        │   ├── PublishView.vue          # ElUpload 多图 + 内容 + 分类
        │   └── DynamicDetail.vue        # 动态详情 + 评论区（两级输入框：@回复）
        ├── chat/
        │   ├── ChatListView.vue         # 会话列表：每15秒刷新对方在线状态；hover显示红色删除按钮 + ElMessageBox 二次确认；会话对方信息用 getUserById
        │   └── ChatRoomView.vue         # ★ 核心：7套主题 / 分类表情包 / 20条懒加载 / 右键菜单；顶部在线状态每2秒轮询 updatePeerOnline()
        └── match/MatchListView.vue      # Tabs：互关 / 我喜欢的 / 喜欢我的
```

---

## ⚙️ vite.config.js 关键配置

```js
export default defineConfig({
  server: {
    port: 5173,
    allowedHosts: true,                      // 🔴 放行所有 Host：ngrok / Tunnel 直接用
    proxy: {
      '/api': {
        target: 'http://localhost:8080',    // 后端端口
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
```

---

## 💎 特色功能详解

### ⭐ ① 默契匹配度（三维实时）

- 调用：`match.js` → `getMatchTacit(uid)`
- 公式：
  - 🧠 **性格契合 40%**：基础 72 分 + 消息互动加成(≤15) + 共同爱好数加成(≤13)
  - 🎨 **兴趣相似 35%**：Jaccard 相似度 = 交集/并集
  - 💬 **聊天频率 25%**：消息条数非线性映射（≥100 条 → 接近满分）
- 展示：`UserDetailView` 环形 `svg`，中心随分动态：
  | 分数 | 中心表情 | 文案 |
  |------|---------|------|
  | ≥90 | 💘 | 超合拍 |
  | ≥75 | 💕 | 很契合 |
  | ≥60 | ✨ | 聊得来 |
  | ≥40 | 🌸 | 多交流 |
  | <40 | 🌱 | 慢慢来 |
- TA 的兴趣：`loadPeerInfo()` → `getUserById(uid)` 取 `hobbies` 字段，与 100+ 常用爱好的 emoji map 查对应；无爱好 → 「TA还没填爱好呢，打个招呼认识下～」空态。

### ⭐ ② 评论系统：仅两层结构（防无限递归）

- 数据处理：`CommentItem.vue` + `DynamicDetail.vue`
  - 每条新回复 → 追溯 `parent_id` 向上 `findRootId()` 直到 `parent_id=null`
  - 统一挂到「根评论」的 `children` 数组下（含 `replyToUserInfo` @用户信息）
- 渲染：一级评论渲染 `.reply-list`，二级节点**不再渲染 children**，避免后端嵌套过深导致递归栈爆。

### ⭐ ③ ChatRoomView：主题 / 表情包 / 懒加载 / 右键

**主题皮肤 7 套**：
```js
const THEME_STORAGE_KEY = 'mf_chat_theme'
const THEMES = [
  { id:'sakura', name:'🌸 樱花粉', vars:{ '--mf-primary':'#F48FB1', ... } },
  { id:'mint',   name:'🍃 薄荷绿', ... },
  { id:'sky',    name:'☁️ 天空蓝', ... },
  { id:'starry', name:'🌌 星夜紫', ... },
  { id:'sun',    name:'☀️ 暖阳橙', ... },
  { id:'latte',  name:'☕ 奶咖色', ... },
  { id:'pure',   name:'⚪ 极简白', ... },
]
const currentThemeId = ref(localStorage.getItem(THEME_STORAGE_KEY) || THEMES[0].id)
function applyThemeVars(id){ /* document.documentElement.style.setProperty 逐个应用 */ }
```
面板 `<Teleport to="body">`，避免父容器 `backdrop-filter: blur(18px)` 创建新包含块导致 position:fixed 飞出去 ⚠️（旧版踩过坑，`right-click menu` 同处理）

**分类表情包 + 最近使用**：
- 数据：`EMOJI_CATEGORIES` 7 组（小黄脸 / 手势 / 动物 / 美食 / 运动 / 物件 / 爱心）≈ 200 emoji
- 最近使用：`localStorage['mf_recent_emojis']`，最多 24 条；发一条写回一次，取 `splice(0,24)`
- 光标插入：`insertEmojiAtCursor(emoji)` 取 `textarea.selectionStart/End`，前后拼 `value = before + emoji + after`；`nextTick(()=>textarea.focus() + 设 selectionStart/End = before+emoji.length)` 保持光标

**历史消息 20 条分页懒加载**：
```js
const PAGE_SIZE = 20
const pageNow = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

// 进入房间首屏拉 20 条 → scrollToBottom()
async function loadMessages() { pageNow.value=1; const r = await getMessages(sessionId, {page:1,pageSize:PAGE_SIZE}) }

// 滚动到顶部（<=30px）自动加载前一页
function onScroll(){
  const el = msgAreaEl.value
  if (loadingMore.value || !hasMore.value) return
  if (el.scrollTop <= 30) loadMoreHistory()
}

async function loadMoreHistory() {
  loadingMore.value = true
  const oldH = el.scrollHeight, oldT = el.scrollTop
  pageNow.value += 1
  const r = await getMessages(sessionId, {page:pageNow.value,pageSize:PAGE_SIZE})
  nextTick(() => { el.scrollTop = oldT + (el.scrollHeight - oldH) }) // 保持位置不跳
  loadingMore.value = false
}
```
- 未读：每 2 秒轮询 `startPolling()` 拉取 `pageSize = pageNow × 20` 同步全量已加载；`wasNearBottom` 判断只有用户靠底才自动滚底贴最新
- 顶部文案三态：⬆️ 上滑查看更早 → ⏳ 加载中 → ✅ 以上就是全部历史消息了

**右键消息菜单（撤回 2 分钟内 / 复制 / 转发）**：
- 气泡 `@contextmenu.prevent="onMsgRightClick($event, msg)"`
- 菜单项条件：撤回 → 自己发的 && `Date.now() - msg.createdAt < 120000`
- 成功后气泡文案改为「你撤回了一条消息」并加灰。
- 整个菜单 `<Teleport to="body">`，`position: fixed; left: clientX; top: clientY`，全局 `document click` 关闭 ⚠️（否则被 `.glass-card` 的 backdrop-filter 创建新包含块导致飞出边界，之前踩过坑）。

---

## 🛰 API 约定

所有接口**统一前缀 `/api`**，返回体结构：

```json
{ "code": 200, "msg": "ok", "data": {} }
```

分页返回体（聊天消息 / 动态 / 用户列表 / 评论等）：

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

- **登录成功**：`data` 字段直接是 satoken 字符串（不是对象）
- **持久化**：`localStorage.satoken` + `localStorage.userInfo` → Pinia 同步
- **请求注入**：`request.js` 拦截器自动 `headers.satoken = satoken`
- **过期处理**：响应拦截器 `code === 401` → `Pinia logout()` + `router.push('/login')` + `ElMessage` 提示

### 限流响应

- **429 Too Many Requests**：`Result.fail(429, "...")`，`ElMessage.error(msg)`
- **503 熔断**：`Result.fail(503, "服务暂不可用，请稍后再试")`，全局 request 拦截统一提示

---

## 🧪 自测清单

1. ✅ 未登录访问 `/home` / `/chat` → 自动跳 `/login`
2. ✅ 登录成功 → `localStorage.satoken` + `Pinia.userInfo` 都有 → 跳 `/home`
3. ✅ 首页 Hero 右侧出现**当前登录用户头像**（无头像时显示昵称首字母兜底圆）
4. ✅ 首页筛选栏「性别 / 城市 / 爱好 / 职业」→ `GET /user/list` 返回分页
5. ✅ 用户详情页「默契度环」中心表情随分数变；「TA 的兴趣」显示真实爱好 emoji
6. ✅ 聊天列表每 15 秒自动刷新对方在线状态；点删除按钮弹出 ElMessageBox 二次确认
7. ✅ 打开聊天室 → 首屏 20 条 → 上滑加载更早 20 条**当前消息不跳**（滚动位置稳定）
8. ✅ 聊天室顶栏对方在线状态每 2 秒轮询
9. ✅ 切换皮肤 → localStorage 持久化 → 刷新页面仍生效
10. ✅ 表情面板点击 → 在光标位置精确插入 + 最近使用面板出现新表情
11. ✅ 右键自己 2 分钟内的消息 → 可撤回 → 气泡样式变化并同步
12. ✅ 响应式测试：缩小窗口到 768px 以下 → 聊天室自动变单列；个人主页横排变竖排不挤出按钮
13. ✅ `npm run build` → 成功出 `dist/`，无报错无 warning

---

## 🛠 常见问题

### `npm install` 卡住

→ 换 npmmirror：`npm config set registry https://registry.npmmirror.com`，再试一次。

### 接口全 404 / Network Error

→ 后端起了吗？`http://localhost:8080/api/actuator/health` 返回 `{"status":"UP"}` 不？DevTools Network 检查请求 URL 是 `http://localhost:5173/api/xxx`？如果是部署环境检查 Nginx `proxy_pass http://127.0.0.1:8080;` 有没有写错。

### 滚动条 / 气泡样式错位

→ 中间滚动区域外层 Flex 务必加 `min-height:0`！否则内容会把尾部挤出视口 ⚠️（项目踩过的坑）。

### 表情面板 / 主题面板 / 右键菜单点了但看不见

→ 按 F12 → Elements，搜 `<body>` 末尾是否有 Teleport 渲染的面板节点（不是 `#app` 的子元素！）。面板显隐靠 `showEmojiPanel.value = true/false`，点击面板外部 document click 会自动关。

### 懒加载后页面跳

→ 确认 `loadMoreHistory` 里 `nextTick` 后再算 `scrollTop += (newHeight - oldHeight)`；量大就再包 `requestAnimationFrame`。

### 穿透访问时提示 Host 不允许

→ 老版本没加 `allowedHosts`，新版已经加了；如仍出现 → 确认 `vite.config.js` 的 `server.allowedHosts` 是 `true`（不要写字符串 `'all'`，在 Vite 里会被当做具体 Host 校验拦截）。

### 个人主页手机端「编辑资料/返回」按钮被挤出屏幕

→ 已改为：`@media (max-width:767px)` 下 `.profile-inner { flex-direction: column }`，按钮容器宽度 100%，两个按钮 `flex:1` 各占一半。

更多部署相关问题，查看项目根 `deploy/DEPLOY_LOCAL_WINDOWS.md`。
