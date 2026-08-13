# MakeFriends 交友平台 💝

一个基于 **Spring Boot 3 + Vue 3** 的现代化交友网站，支持用户注册登录、个人资料展示、动态发布与互动、用户匹配（喜欢 / 互关）、实时聊天（7 套皮肤主题 + 分类表情包 + 历史消息分页懒加载）等核心功能。

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-6DB33F?logo=spring-boot)
![Vue 3](https://img.shields.io/badge/Vue-3.x-42B883?logo=vue.js)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite)
![License](https://img.shields.io/badge/license-Learn%20Only-blue)

---

## ✨ 功能一览

| 模块 | 核心能力 |
|---|---|
| 🔐 登录注册 | 手机号 + 密码注册 / 登录，Sa-Token 会话管理 |
| 👤 用户中心 | 头像 / 昵称 / 性别 / 生日 / 城市 / 个人简介 / 爱好标签 / 职业，编辑资料 |
| 💘 匹配机制 | 喜欢 / 取消喜欢 / 被喜欢 / 互关列表，双向喜欢自动建会话 |
| 📝 动态广场 | 图文动态发布、图片上传、点赞 / 取消、二级评论、审核状态 |
| 💬 实时聊天 | WebSocket 推送 / HTTP 双保险轮询、未读计数、7 套皮肤主题、分类表情包（最近使用）、历史消息 20 条分页懒加载、右键撤回（2 分钟内） |
| 🔍 用户筛选 | 按性别 / 城市 / 爱好 / 职业 / 年龄筛选推荐用户卡片 |
| 📁 文件上传 | 头像 / 动态图上传到本地 uploads/，静态资源映射 |

---

## 📚 目录

- [技术栈](#-技术栈)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [测试账号](#-测试账号)
- [项目结构](#-项目结构)
- [API 接口文档](#-api-接口文档)
- [聊天室特色功能](#-聊天室特色功能)
- [数据库表结构](#-数据库表结构)
- [生产部署建议](#-生产部署建议)
- [常见问题](#-常见问题)

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 说明 |
|---|---|---|
| Java | 17 | LTS 长期支持版 |
| Spring Boot | 3.3.x | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架，自动 CRUD、分页插件 |
| Sa-Token | 1.38.0 | 登录认证（比 Spring Security 更轻量） |
| MySQL | 8.0.x | 关系型数据库 |
| WebSocket | - | 实时消息推送通道 |
| SpringDoc OpenAPI | 2.6.0 | Swagger 接口文档 |
| Lombok | - | 简化 POJO 代码 |

### 前端

| 技术 | 版本 | 说明 |
|---|---|---|
| Vue | 3.x | `<script setup>` 组合式 API |
| Vite | 5.x | 构建工具（冷启动毫秒级） |
| Element Plus | 2.14+ | UI 组件库 |
| Pinia | - | 全局状态管理（用户信息 / Token） |
| Axios | - | HTTP 请求封装（带 Token 拦截器 + 401 重定向） |
| Vue Router | 4.x | 路由管理（含登录守卫） |
| Sass | - | CSS 变量主题 / 气泡样式 |

---

## 📦 环境要求

| 软件 | 最低版本 | 验证命令 | 下载 |
|---|---|---|---|
| JDK | 17 | `java -version` | https://adoptium.net/zh-CN/temurin/releases/ |
| Maven | 3.9 | `mvn -v` | https://maven.apache.org/download.cgi |
| Node.js | 20 | `node -v` / `npm -v` | https://nodejs.org/ |
| MySQL | 8.0 | `Get-Service MySQL80` | https://dev.mysql.com/downloads/installer/ |

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
CREATE DATABASE IF NOT EXISTS make_friends
    DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE make_friends;
```

命令行导入 SQL（顺序不能反）：

```powershell
mysql -u root -p make_friends < make-friends-backend/sql/makefriends_schema.sql
mysql -u root -p make_friends < make-friends-backend/sql/003_hobby_occupation_dict.sql
```

### 2. 修改后端配置

编辑 `make-friends-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/make_friends?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你自己的 MySQL root 密码    # ← 修改这里

upload:
  path: E:/TraePorject/make-friends/make-friends-backend/uploads   # ← 本机绝对路径
```

### 3. 启动后端

```powershell
# 可选：配置本机 JDK / Maven 路径
$env:JAVA_HOME = "D:\JDK"
$env:MAVEN_HOME = "D:\apache-maven-3.9.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

cd make-friends-backend
mvn spring-boot:run
```

✅ 成功标志：访问 http://localhost:8080/api/actuator/health → `{"status":"UP"}`

### 4. 启动前端（新开一个 PowerShell）

```powershell
cd make-friends-frontend
npm install      # 仅首次需要
npm run dev
```

✅ 成功标志：终端打印 `➜  Local:   http://localhost:5173/`

### 5. 访问网站 → http://localhost:5173/

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
├── README.md
├── .gitignore
├── make-friends-backend/               # ☕ Spring Boot 后端
│   ├── README.md                       # 后端专用文档
│   ├── pom.xml
│   ├── sql/                            # 9 张表结构 + 字典数据
│   ├── uploads/                        # 上传目录（运行时自动创建）
│   └── src/main/
│       ├── java/com/makefriends/
│       │   ├── MakeFriendsApplication.java
│       │   ├── common/                 # Result / 全局异常
│       │   ├── config/                 # MP / Sa-Token / CORS / WebSocket
│       │   ├── controller/             # 10 个 Controller
│       │   ├── service/ + impl/        # 7 个 Service
│       │   ├── mapper/                 # 9 个 Mapper
│       │   ├── entity/                 # 9 个 Entity
│       │   ├── dto/ + vo/
│       │   ├── websocket/              # ChatWebSocketHandler
│       │   └── util/
│       └── resources/application.yml
│
└── make-friends-frontend/              # 💚 Vue 3 前端
    ├── README.md
    ├── package.json
    ├── vite.config.js                  # Vite + /api 代理到 8080
    └── src/
        ├── api/                        # auth / user / match / dynamic / chat / upload
        ├── components/                 # UserCard / DynamicCard / CommentItem
        ├── layouts/MainLayout.vue
        ├── router/ + stores/user.js
        ├── utils/format.js
        └── views/
            ├── auth/ (login/register)
            ├── home/
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
| 🔐 Auth | POST /auth/register · POST /auth/login · POST /auth/logout | 登录注册 | ❌/❌/✅ |
| 👤 User | GET /user/me · PUT /user/me · GET /user/{id} · GET /user/list | 用户信息 | ✅ |
| 💘 Match | POST /match/like/{uid} · DELETE /match/unlike/{uid} · GET /match/status/{uid} · GET /match/my-likes · /who-likes-me · /mutual | 喜欢/互关 | ✅ |
| 📝 Dynamic | POST /dynamic · DELETE /{id} · GET /list · GET /{id} · GET /user/{uid} · GET /my | 动态 CRUD | ✅ |
| 💬 Comment | POST /comment · DELETE /{id} · GET /list/{dynamicId} | 二级评论 | ✅ |
| 👍 Like | POST /like/toggle/{did} · GET /check/{did} | 点赞去重 | ✅ |
| 💬 Chat | GET /sessions · GET /messages/{sid}?page=&pageSize= · POST /send · PUT /read/{sid} · GET /unread-count | 聊天 + 分页消息 | ✅ |
| 📤 Upload | POST /upload/image | 图片上传 | ✅ |

WebSocket 推送：`ws://localhost:8080/api/ws/chat?satoken=xxx`

---

## 💎 聊天室特色功能

### ① 7 套皮肤主题（持久化）

通过 CSS 变量 + localStorage。内置：樱花粉 / 薄荷绿 / 天空蓝 / 星夜紫 / 暖阳橙 / 奶咖色 / 极简白。

### ② 分类表情包 + 最近使用

`<Teleport to="body">` 挂载避免 overflow 截断。7 个分类（小黄脸 / 手势 / 动物 / 美食 / 运动 / 物件 / 爱心）+ 最近使用 24 条。光标位置精确插入。

### ③ 历史消息分页懒加载

首屏 20 条，上滑触顶（≤30px）自动 prepend 前 20 条并保持滚动位置不跳。顶部提示三态：⬆️ 上滑查看更早 → ⏳ 加载中 → ✅ 以上就是全部历史消息了。

### ④ 右键消息菜单

气泡点右键：复制 / 转发 / 撤回（2 分钟内 · 自己发的）。

---

## 🗄 数据库表结构

完整 DDL：`make-friends-backend/sql/makefriends_schema.sql`（通过 MySQL `SHOW CREATE TABLE` 真实导出）

| 表名 | 说明 | 核心列 |
|---|---|---|
| users | 用户表 | id, phone, password, nickname, avatar, gender, birthday, city, hobby, occupation, intro, online_status, last_active_at |
| user_matches | 匹配关系 | user_id, target_user_id, match_type (1=喜欢 / 2=取消 / 3=互关) |
| chat_sessions | 会话 | user1_id, user2_id, user1_deleted, user2_deleted, last_msg, last_msg_at |
| chat_messages | 聊天消息 | session_id, sender_id, receiver_id, msg_type(1-4), content, is_read, is_withdrawn |
| user_dynamics | 动态 | user_id, content, images, like_count, comment_count, status (0=正常 / 2=审核中) |
| dynamic_comments | 评论 | dynamic_id, user_id, content, parent_id, reply_to_user_id |
| dynamic_likes | 点赞 | dynamic_id, user_id; 唯一索引 uk_dynamic_user 防重复 |
| hobby_dict | 爱好字典 | 180+ 条，name + category |
| occupation_dict | 职业字典 | 200+ 条，name + category |

---

## ☁️ 生产部署建议

### 后端打包成 jar

```powershell
cd make-friends-backend
mvn clean package -DskipTests
# → target/make-friends-backend-0.0.1-SNAPSHOT.jar
```

服务器上运行：

```bash
nohup java -jar make-friends-backend-0.0.1-SNAPSHOT.jar \
  --spring.datasource.password='线上密码' > app.log 2>&1 &
```

### 前端打包成 dist

```powershell
cd make-friends-frontend
npm run build
# → dist/ 上传到 Nginx
```

Nginx 配置（关键片段）：

```nginx
location / {
    root /usr/share/nginx/html/makefriends;
    index index.html;
    try_files $uri $uri/ /index.html;     # Vue history 模式
}
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;   # WebSocket 必需
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400;
}
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

→ 检查 `application.yml` 的 `upload.path` 是否是启动机器上的真实绝对路径，以及 `WebConfig` 的资源映射。

---

## 📝 许可证

本项目仅供**学习交流**使用，请勿用于商业用途。
