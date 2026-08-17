# 搭伴 (rokbj.me) · 校园青年互动社区 💝

> **校园好搭伴，生活不孤单**
>
> 一个基于 **Spring Boot 3 + Vue 3** 的现代化校园互动平台。集交友认识、动态分享、互动评论、默契匹配、实时聊天等功能于一体，未来可扩展二手交易、拼单搭伴、校园日常等多元场景。
>
> 🌍 线上演示：**https://rokbj.me**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-6DB33F?logo=spring-boot)
![Vue 3](https://img.shields.io/badge/Vue-3.x-42B883?logo=vue.js)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite)
![Cloudflare](https://img.shields.io/badge/Cloudflare-Tunnel-E57E22?logo=cloudflare)
![License](https://img.shields.io/badge/license-Learn%20Only-blue)

---

## ✨ 功能一览

| 模块 | 核心能力 |
|---|---|
| 🔐 登录注册 | 手机号 + 密码注册 / 登录，Sa-Token 会话管理 |
| 👤 用户中心 | 头像 / 昵称 / 性别 / 生日 / 城市 / 个人简介 / 爱好标签 / 职业，编辑资料 |
| 💘 默契匹配 | 喜欢 / 取消喜欢 / 被喜欢 / 互关列表；**三维默契度算法**（性格契合40%+兴趣相似35%+聊天频率25%）；双向喜欢自动建会话 |
| 📝 动态广场 | 图文动态发布、图片上传、点赞 / 取消、**二级评论系统**（仅两层结构）、审核状态 |
| 💬 实时聊天 | WebSocket 推送 / HTTP 双保险轮询、未读计数、**7 套皮肤主题**、**分类表情包**（最近使用）、**历史消息 20 条分页懒加载**、右键撤回（2 分钟内） |
| 🔍 用户筛选 | 按性别 / 城市 / 爱好 / 职业 / 年龄筛选推荐用户卡片 |
| 📁 文件上传 | 头像 / 动态图上传到本地 uploads/，静态资源映射 |
| 🛡 安全防护 | **第①层**：Redis 缓存热点数据 + Sa-Token Redis 持久化；**第②层**：HikariCP 连接池（50连接）+ MySQL 复合索引；**第③层**：Resilience4j 熔断 / 重试 + IP 限流（180次/分钟） + 登录失败锁定（5次/60秒→锁10分钟） |
| 🌍 一键外网部署 | Cloudflare Tunnel 免费内网穿透，配合 Nginx 反向代理，本地电脑即可公网访问 **https://rokbj.me** |

> 🔮 未来规划（等你命令才做）：二手交易市场、拼单/搭饭/搭自习、校园活动、失物招领等校园场景。

---

## 📚 目录

- [技术栈](#-技术栈)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [测试账号](#-测试账号)
- [项目结构](#-项目结构)
- [API 接口文档](#-api-接口文档)
- [特色功能亮点](#-特色功能亮点)
- [数据库表结构](#-数据库表结构)
- [生产部署](#-生产部署) ⭐
- [常见问题](#-常见问题)

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 说明 |
|---|---|---|
| Java | 17 | LTS 长期支持版 |
| Spring Boot | 3.3.x | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架，自动 CRUD、分页插件 |
| Sa-Token | 1.38.0 | 登录认证，**Redis 持久化存储**（sa-token-redis-jackson） |
| MySQL | 8.0.x | 关系型数据库，复合覆盖索引优化查询 |
| Redis | 7+ | 缓存热点数据 + Sa-Token Session + 限流计数器 |
| WebSocket | - | 实时消息推送通道 |
| Resilience4j | 2.2.0 | **熔断器**（失败率≥50%触发，20秒半开） + **重试**（最多2次） |
| Spring AOP | - | 支撑 Resilience4j 注解切面 |
| SpringDoc OpenAPI | 2.6.0 | Swagger 接口文档 |
| HikariCP | - | Spring Boot 自带连接池，`maximum-pool-size=50` 高并发优化 |

### 前端

| 技术 | 版本 | 说明 |
|---|---|---|
| Vue | 3.x | `<script setup>` 组合式 API |
| Vite | 5.x | 构建工具（冷启动毫秒级），**allowedHosts=true** 支持 ngrok / Tunnel |
| Element Plus | 2.14+ | UI 组件库，粉蓝（#5B8DEF → #FFB5C5）全新配色导航栏 |
| Pinia | - | 全局状态管理（用户信息 / Token） |
| Axios | - | HTTP 请求封装（带 Token 拦截器 + 401 重定向） |
| Vue Router | 4.x | 路由管理（含登录守卫） |
| Sass | - | CSS 变量主题 / 气泡样式，响应式 4 级断点 |

---

## 📦 环境要求

| 软件 | 最低版本 | 验证命令 | 下载 |
|---|---|---|---|
| JDK | 17 | `java -version` | https://adoptium.net/zh-CN/temurin/releases/ |
| Maven | 3.9 | `mvn -v` | https://maven.apache.org/download.cgi |
| Node.js | 20 | `node -v` / `npm -v` | https://nodejs.org/ |
| MySQL | 8.0 | `Get-Service MySQL*` | https://dev.mysql.com/downloads/installer/ |
| Redis | 7.0 | `redis-cli ping` | Windows: https://github.com/tporadowski/redis/releases |
| cloudflared (可选) | 最新版 | `cloudflared --version` | 用于公网穿透 https://github.com/cloudflare/cloudflared/releases |

**Maven 阿里云镜像加速**：编辑 `conf/settings.xml` 的 `<mirrors>`：

```xml
<mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

**npm 国内镜像**：`npm config set registry https://registry.npmmirror.com`

---

## 🚀 快速开始

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS makefriends
    DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE makefriends;
```

命令行导入 SQL（顺序不能反）：

```powershell
mysql -u root -p makefriends < make-friends-backend/sql/makefriends_schema.sql
mysql -u root -p makefriends < make-friends-backend/sql/004_db_indexes.sql
```

### 2. 检查 Redis 是否启动

```powershell
redis-cli ping
# 返回 PONG 说明正常；否则启动：
# cd D:\Redis-8.6.4 ; .\redis-server.exe
```

### 3. 修改后端配置

编辑 `make-friends-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/makefriends?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你自己的 MySQL root 密码    # ← 修改这里
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # password: 你的Redis密码（没有就留空）

file:
  upload:
    path: E:/TraePorject/make-friends/uploads/   # ← 本机绝对路径，用 / 分隔
```

### 4. 启动后端

```powershell
# 可选：配置本机 JDK / Maven 路径
$env:JAVA_HOME = "D:\JDK"
$env:MAVEN_HOME = "D:\apache-maven-3.9.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

cd make-friends-backend
mvn spring-boot:run
```

✅ 成功标志：控制台出现 `Started MakeFriendsApplication`

### 5. 启动前端（新开一个 PowerShell）

```powershell
cd make-friends-frontend
npm install      # 仅首次需要
npm run dev
```

✅ 成功标志：终端打印 `➜  Local:   http://localhost:5173/`

### 6. 访问网站 → http://localhost:5173/

---

## 🧪 测试账号

**密码统一为 `Test123456`**：

| 手机号 | 昵称 | 性别 | 城市 | 备注 |
|---|---|---|---|---|
| 13800000001 | Alice | 女 | 武汉 | UI 设计师，有动态 & 评论 |
| 13800000002 | Bob | 男 | 北京 | 程序员 |
| 13800000003 | Cici | 女 | 上海 | 设计师 |
| 13800000004 | David | 男 | 上海 | 金融分析师 |
| 13800000005 | Emma | 女 | 广州 | 大学生 |
| 13800000006 | Frank | 男 | 深圳 | 系统管理员 |

---

## 🗂 项目结构

```
make-friends/
├── README.md                              # ⭐ 本文件
├── .gitignore
│
├── deploy/                                # ⭐ 部署文档 + 一键脚本（必看！）
│   ├── DEPLOY_LOCAL_WINDOWS.md            # 本地电脑 + Cloudflare Tunnel 完整部署手册
│   ├── start-all.ps1                      # ✨ 一键启动所有服务
│   ├── stop-all.ps1                       # ✨ 一键停止所有服务
│   ├── status-all.ps1                     # ✨ 查看所有服务状态
│   ├── nginx.conf                         # Nginx Windows 版配置（复制到 D:\nginx\conf\）
│   ├── cloudflared-config.yml             # Cloudflare Tunnel 配置模板
│   ├── application-prod.yml               # 后端生产环境配置示例
│   ├── DEPLOY_GUIDE_CLOUD.md              # 阿里云 Linux 服务器方案（备选）
│   └── DEPLOY_GUIDE_WINDOWS.md            # 阿里云 Windows Server 方案（备选）
│
├── make-friends-backend/                  # ☕ Spring Boot 后端
│   ├── README.md                          # 后端专用文档
│   ├── pom.xml
│   ├── sql/                               # 9 张表结构 + 复合索引 SQL
│   │   ├── makefriends_schema.sql
│   │   └── 004_db_indexes.sql
│   └── src/main/
│       ├── java/com/makefriends/
│       │   ├── MakeFriendsApplication.java
│       │   ├── common/                    # Result / 全局异常 / Redis缓存 / 限流 / 熔断异常
│       │   ├── config/                    # MP / Sa-Token / CORS / WebSocket / 限流拦截器(最高优先级order=0)
│       │   ├── controller/                # 10 个 Controller
│       │   ├── service/ + impl/           # 带 @CircuitBreaker @Retry 熔断重试注解
│       │   ├── mapper/                    # 9 个 Mapper
│       │   ├── entity/                    # 9 个 Entity
│       │   ├── dto/ + vo/
│       │   ├── websocket/                 # ChatWebSocketHandler (ConcurrentHashMap 线程安全)
│       │   └── util/
│       └── resources/application.yml
│
└── make-friends-frontend/                 # 💚 Vue 3 前端
    ├── README.md
    ├── package.json
    ├── vite.config.js                     # Vite + allowedHosts=true + /api 代理到 8080
    └── src/
        ├── api/                           # auth / user / match / dynamic / chat / upload
        ├── components/                    # UserCard / DynamicCard / CommentItem
        ├── layouts/MainLayout.vue         # 全新搭伴导航栏：轻盈粉蓝渐变下划线 + 发布按钮
        ├── router/ + stores/user.js
        ├── utils/format.js
        └── views/
            ├── auth/ (login/register)
            ├── home/ (Hero 右侧显示当前登录用户头像 + 渐变光晕)
            ├── user/ (profile + edit + UserHomeView)
            ├── dynamic/ (list + publish + detail)
            ├── chat/ (ChatListView + ChatRoomView)
            └── match/
```

---

## 📡 API 接口文档

启动后端后访问 Swagger UI：**http://localhost:8080/api/swagger-ui.html**

| 模块 | 接口（统一前缀 `/api`） | 说明 | 鉴权 |
|---|---|---|---|
| 🔐 Auth | POST /auth/register · POST /auth/login · POST /auth/logout | 登录注册；**第3次错误开始计数→5次锁10分钟** | ❌/❌/✅ |
| 👤 User | GET /user/me · PUT /user/me · GET /user/{id} · GET /user/list | 用户资料（敏感字段 phone/password/status 禁止修改） | ✅ |
| 💘 Match | POST /match/like/{uid} · DELETE /match/unlike/{uid} · GET /match/status/{uid} · GET /match/my-likes · /who-likes-me · /mutual · /tacit/{uid} | 喜欢/互关/三维默契度 | ✅ |
| 📝 Dynamic | POST /dynamic · DELETE /{id} · GET /list · GET /{id} · GET /user/{uid} · GET /my | 动态 CRUD（@CircuitBreaker + @Retry） | ✅ |
| 💬 Comment | POST /comment · DELETE /{id} · GET /list/{dynamicId} | 二级评论（findRootId统一挂根节点） | ✅ |
| 👍 Like | POST /like/toggle/{did} · GET /check/{did} | 点赞去重 | ✅ |
| 💬 Chat | GET /sessions · GET /messages/{sid}?page=&pageSize= · POST /send · PUT /read/{sid} · GET /unread-count · DELETE /session/{sid} | 聊天+分页消息（20条/页，@Transactional + 熔断） | ✅ |
| 📤 Upload | POST /upload/image | 图片上传（绝对路径 ${user.dir}/uploads/，返回 /api/files/ 前缀） | ✅ |

- **IP 限流**：单 IP 每分钟 180 次上限（Redis `incrEx` 分桶计数），超限返回 429 Too Many Requests
- **熔断降级**：关键写操作失败率≥50%→熔断，20s后半开试探；失败最多重试 2 次
- **WebSocket 推送**：`ws://localhost:8080/api/ws/chat?satoken=xxx`

---

## 💎 特色功能亮点

### ⭐① 默契匹配度（三维实时算法）

| 维度 | 权重 | 算法 |
|------|------|------|
| 🧠 性格契合 | 40% | 基础 72 分 + 消息互动加成(≤15) + 共同爱好加成(≤13) |
| 🎨 兴趣相似 | 35% | Jaccard 相似度 |
| 💬 聊天频率 | 25% | 基于消息条数的非线性映射 |

环形中心表情和文字动态变化：≥90💘超合拍 / ≥75💕很契合 / ≥60✨聊得来 / ≥40🌸多交流 / <40🌱慢慢来

### ⭐② 三层防护架构（高并发必备）

```
用户请求
   │
   ▼
Layer ③ [限流 / 登录锁] GlobalRateLimitFilter (order=0, 最高优先级)
     ├─ IP级 180次/分钟 (Redis分桶)
     └─ 登录失败 5次/60秒 → 锁定10分钟
   │
   ▼
Layer ② [数据库层保护]
     ├─ HikariCP 50连接池
     ├─ MySQL 5张表复合覆盖索引
     └─ Resilience4j @CircuitBreaker(失败率≥50%) + @Retry(最多2次)
   │
   ▼
Layer ① [Redis 缓存] Cache-Aside Pattern
     ├─ Sa-Token token 持久化到 Redis (sa-token.type=redis)
     ├─ 用户详情/字典数据缓存
     └─ 限流计数器 Redis.incrEx
```

### ⭐③ 聊天室（7主题+表情包+懒加载）

- **主题皮肤**：樱花粉 / 薄荷绿 / 天空蓝 / 星夜紫 / 暖阳橙 / 奶咖色 / 极简白。CSS 变量 + localStorage 持久化。面板 `<Teleport to="body">` 避免截断
- **分类表情包**：7 个分类 × 近 200 emoji + 最近使用 24 条。`textarea.selectionStart` 精确光标插入
- **懒加载分页**：首屏 20 条，上滑触顶 prepend 前 20 条，记录 oldHeight 保持滚动位置稳定。顶部提示三态（上滑/加载/已到顶）
- **右键菜单**：复制 / 转发 / 撤回（2分钟内，自己发的）。气泡消息用 `<Teleport to="body">` 渲染，避免 `.glass-card` 元素 `backdrop-filter` 创建新包含块导致 fixed 定位异常

### ⭐④ 全新视觉：粉蓝品牌「搭伴」

- **Logo & 品牌名**：原 MakeFriends → 新「搭伴」。slogan：**校园好搭伴，生活不孤单**（为未来扩展二手交易/拼单/校园日常预留空间）
- **导航栏 2026 版**：几何胶囊图标「搭」+ 菜单项轻盈 hover 灰底 + active 粉蓝渐变下划线（#5B8DEF → #FFB5C5）+ 「＋发布」快捷按钮（渐变 hover 上浮）
- **Home Hero**：右侧纯装饰圆改为「当前登录用户头像」，粉蓝渐变光晕呼吸动画 + 无头像时兜底「昵称首字母 + 粉蓝渐变圆」
- **响应式 4 级断点**：≥1024px 桌面 / 768–1023px 平板 / 480–767px 手机 / ≤479px 极限小屏。关键布局（聊天双栏、个人主页横排、登录卡片）均降级优化
- **聊天栏宽度弹性约束**：grid 列分配中间栏封顶（不写死宽度）+ 内层 clamp() 变量实现大屏适中两侧留白

### ⭐⑤ 本地秒变公网 🌍

Cloudflare Tunnel + Nginx + 一键 PowerShell 脚本，5 分钟把本地网站发布到 **https://rokbj.me**，免费 HTTPS、全球 CDN、无需公网 IP、无需路由器配置。详见 `deploy/DEPLOY_LOCAL_WINDOWS.md`。

---

## 🗄 数据库表结构

完整 DDL：`make-friends-backend/sql/makefriends_schema.sql` + `004_db_indexes.sql`（通过 MySQL `SHOW CREATE TABLE` 真实导出）

| 表名 | 说明 | 核心索引 |
|---|---|---|
| users | 用户表 | `uk_users_phone` 手机号唯一；online_status + last_active_at 在线判定 |
| user_matches | 匹配关系 | user_id, target_user_id, match_type (1=喜欢 / 2=取消 / 3=互关) |
| chat_sessions | 会话 | user1_id / user2_id（min/max 生成规则）；user1_deleted / user2_deleted 双删除标记；**idx_session_created** (session_id, created_at DESC) |
| chat_messages | 聊天消息 | session_id, sender_id, receiver_id, msg_type(1=text/2=image), is_read, is_withdrawn；**idx_session_receiver_read** 三列复合索引 |
| user_dynamics | 动态 | user_id, content, images, like_count, comment_count, status；**idx_user_created / idx_status_created / idx_dynamic_created** 三个复合索引 |
| dynamic_comments | 评论 | dynamic_id, user_id, content, parent_id, reply_to_user_id；仅两层（parent_id / root_id 追溯展平） |
| dynamic_likes | 点赞 | uk_dynamic_user 唯一索引防重复 |
| hobby_dict | 爱好字典 | 180+ 条，name + category，100+ 常用爱好配专属 emoji |
| occupation_dict | 职业字典 | 200+ 条，name + category |

---

## ☁️ 生产部署

### 👉 方案 A：本地电脑 + Cloudflare Tunnel（免费，适合开发/小流量）⭐ 当前线上

完整步骤 + 一键脚本详见 `deploy/DEPLOY_LOCAL_WINDOWS.md`。

**日常使用（管理员 PowerShell）**：

```powershell
cd E:\TraePorject\make-friends\deploy

.\start-all.ps1    # 一键启动: Redis→MySQL→后端→Nginx→Cloudflare Tunnel
.\status-all.ps1   # 一键检查: 进程/端口/HTTP/文件 全维度
.\stop-all.ps1     # 一键停止
```

访问 **https://rokbj.me** 即可。

### 👉 方案 B：阿里云服务器（正式运营）

Linux 版见 `deploy/DEPLOY_GUIDE_CLOUD.md`；
Windows Server 版见 `deploy/DEPLOY_GUIDE_WINDOWS.md`。

### 打包命令

```powershell
# 后端
cd make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests
# → target/make-friends-backend-1.0.0.jar

# 前端
cd ..\make-friends-frontend
npm run build
# → dist/（交给 Nginx root）
```

---

## ❓ 常见问题

### Q1: 后端启动 "Communications link failure"

→ 检查 MySQL 服务是否启动 + `application.yml` 密码是否正确 + 3306 端口是否在监听。

### Q2: 后端启动 "Address already in use"

→ `netstat -ano | findstr :8080` → `taskkill /PID <PID> /F`，或改 `server.port`。

### Q3: 前端接口 401 / Network Error

→ 先访问 http://localhost:8080/api/actuator/health 确认后端 UP → 清 localStorage → 重新登录 → 查 `vite.config.js` 的 proxy target。

### Q4: 前端 Module not found

→ 删 `node_modules` 重新 `npm install`。

### Q5: 上传图片刷新 404

→ 检查 `application.yml` 的 `file.upload.path` 是否是启动机器上的真实绝对路径，以及 `WebConfig` 里是否排除了 Sa-Token 拦截 `/api/files/**`。

### Q6: 聊天 WebSocket 连接失败

→ Nginx 必须加 `Upgrade` + `Connection upgrade` 头 + `proxy_read_timeout 86400s`。见 `deploy/nginx.conf`。

### Q7: 外网 rokbj.me 打不开 / 502

→ 先跑 `deploy/status-all.ps1` 看服务状态 → 再检查 `cloudflared tunnel run rokbj` 窗口是否有 `Connection registered`。**Tunnel 窗口不能关！**

---

## 📝 许可证

本项目仅供**学习交流**使用，请勿用于商业用途。
