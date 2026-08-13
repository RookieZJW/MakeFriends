# MakeFriends 交友平台

一个基于 Spring Boot 3 + Vue 3 的现代化交友网站，支持用户注册登录、个人资料展示、动态发布与互动、用户匹配（喜欢/互关）、实时聊天等核心功能。

---

## 目录

- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
  - [1. 环境准备](#1-环境准备)
  - [2. 数据库配置](#2-数据库配置)
  - [3. 启动后端](#3-启动后端)
  - [4. 启动前端](#4-启动前端)
  - [5. 访问网站](#5-访问网站)
- [测试账号](#测试账号)
- [项目结构](#项目结构)
- [API 接口文档](#api-接口文档)
- [常见问题](#常见问题)

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 长期支持版 |
| Spring Boot | 3.3.x | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架，自动 CRUD |
| Sa-Token | 1.38.0 | 登录认证（比 Spring Security 简单10倍） |
| MySQL | 8.0.x | 数据库 |
| WebSocket | - | 实时聊天推送 |
| SpringDoc | 2.6.0 | Swagger 接口文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3 | 前端框架 |
| Vite | 5 | 构建工具（启动超快） |
| Element Plus | 2.14+ | UI 组件库 |
| Pinia | - | 状态管理 |
| Axios | - | HTTP 请求 |
| Vue Router | 4 | 路由管理 |
| Sass | - | CSS 预处理 |

---

## 环境要求

在开始之前，请确保你的电脑已安装以下软件：

### 1. JDK 17（必须）

```powershell
# 验证是否已安装
java -version
```

**应该输出：** `java version "17.0.x"`

如果未安装，请下载安装：
- 下载地址：https://adoptium.net/zh-CN/temurin/releases/
- 选择：版本 17、Windows、x64、JDK、.msi 安装包
- 安装后配置环境变量 `JAVA_HOME` 指向安装目录

### 2. Maven 3.9+（必须）

```powershell
# 验证是否已安装
mvn -v
```

**应该输出：** `Apache Maven 3.9.x`

如果未安装，请下载配置：
- 下载地址：https://maven.apache.org/download.cgi
- 解压到 `D:\apache-maven-3.9.14`
- 配置环境变量 `MAVEN_HOME = D:\apache-maven-3.9.14`
- PATH 中添加 `%MAVEN_HOME%\bin`

**配置阿里云镜像（加速下载）：**
打开 `D:\apache-maven-3.9.14\conf\settings.xml`，在 `<mirrors>` 标签中添加：

```xml
<mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 3. Node.js（必须）

```powershell
# 验证是否已安装
node -v
npm -v
```

**应该输出：** `v20.x.x` 和 `10.x.x`

如果未安装，请下载安装：
- 下载地址：https://nodejs.org/（选 LTS 版本）
- 配置国内镜像：`npm config set registry https://registry.npmmirror.com`

### 4. MySQL 8.0（必须）

```powershell
# 验证 MySQL 服务是否已启动
Get-Service MySQL80
```

**应该输出：** `Running`

如果未安装，请下载安装：
- 下载地址：https://dev.mysql.com/downloads/installer/
- 安装时设置 root 密码

---

## 快速开始

### 1. 环境准备

确保以下服务都已正常运行：

```powershell
# 检查 MySQL 服务
Get-Service MySQL80

# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -v

# 检查 Node.js 版本
node -v
```

### 2. 数据库配置

本项目的数据库是 `make_friends`，包含 7 张表（users, user_dynamics, dynamic_comments, dynamic_likes, user_matches, chat_sessions, chat_messages），已有测试数据。

**数据库连接参数：**

| 参数 | 值 |
|------|------|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库名 | make_friends |
| 用户名 | root |
| 密码 | Zhang20. |

> **注意：** 如果数据库不存在，请先登录 MySQL 创建：
> ```sql
> CREATE DATABASE IF NOT EXISTS make_friends DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
> ```

### 3. 启动后端

使用 **PowerShell** 执行以下命令（**不要用 CMD**）：

```powershell
# 设置环境变量
$env:JAVA_HOME = "D:\JDK"
$env:MAVEN_HOME = "D:\apache-maven-3.9.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

# 进入后端项目目录
cd E:\TraePorject\make-friends\make-friends-backend

# 启动后端（第一次运行会自动下载依赖，会慢一些）
mvn spring-boot:run
```

**启动成功的标志：**
```
Tomcat started on port 8080 (http) with context path '/api'
Started MakeFriendsApplication in 3.xxx seconds
```

**验证后端是否启动成功：**
打开浏览器访问：http://localhost:8080/api/actuator/health
看到 `{"status":"UP"}` 就成功了。

### 4. 启动前端

**打开新的 PowerShell 窗口**（不要关闭后端窗口），执行：

```powershell
# 进入前端项目目录
cd E:\TraePorject\make-friends\make-friends-frontend

# 安装依赖（仅第一次需要）
npm install

# 启动开发服务器
npm run dev
```

**启动成功的标志：**
```
VITE v8.x.x  ready in xxx ms
➜  Local:   http://localhost:5173/
```

### 5. 访问网站

打开浏览器，访问 **http://localhost:5173/**

---

## 测试账号

数据库已有 6 个测试用户，密码统一为 `Test123456`：

| 手机号 | 昵称 | 性别 | 城市 | 备注 |
|--------|------|------|------|------|
| 13800000001 | Alice | 女 | 武汉 | UI设计师，有动态/评论 |
| 13800000002 | Bob | 男 | 北京 | 程序员 |
| 13800000003 | Cici | 女 | 上海 | 设计师 |
| 13800000004 | David | 男 | 上海 | 金融分析师 |
| 13800000005 | Emma | 女 | 广州 | 大学生 |
| 13800000006 | Frank | 男 | 深圳 | 系统管理员 |

> 你也可以用手机号 13777777777 或 13900000001（密码 Test123456）登录，这是开发过程中注册的测试账号。

---

## 项目结构

### 后端结构

```
make-friends-backend/
├── src/main/java/com/makefriends/
│   ├── MakeFriendsApplication.java      # 启动类
│   ├── common/                           # 公共模块
│   │   ├── Result.java                   # 统一返回结果
│   │   └── GlobalExceptionHandler.java   # 全局异常处理
│   ├── config/                           # 配置类
│   │   ├── MyBatisPlusConfig.java        # 分页插件
│   │   ├── SaTokenConfig.java            # 登录认证
│   │   ├── WebConfig.java                # 跨域配置
│   │   └── WebSocketConfig.java          # WebSocket
│   ├── controller/                       # 控制层
│   │   ├── AuthController.java           # 登录注册
│   │   ├── UserController.java           # 用户信息
│   │   ├── UserMatchController.java      # 匹配
│   │   ├── DynamicController.java        # 动态
│   │   ├── CommentController.java        # 评论
│   │   ├── LikeController.java           # 点赞
│   │   ├── ChatController.java           # 聊天
│   │   └── UploadController.java         # 上传
│   ├── service/                          # 业务逻辑
│   │   ├── impl/                         # 实现类
│   │   └── *.java                        # 接口
│   ├── mapper/                           # 数据库访问
│   ├── entity/                           # 实体类
│   ├── dto/                              # 请求参数
│   ├── vo/                               # 返回视图
│   ├── websocket/                        # WebSocket
│   └── util/                             # 工具类
│
├── src/main/resources/
│   └── application.yml                   # 配置文件
├── uploads/                              # 上传文件目录
└── pom.xml
```

### 前端结构

```
make-friends-frontend/
├── src/
│   ├── api/                    # 后端接口调用
│   │   ├── request.js          # axios 封装
│   │   ├── auth.js             # 登录注册
│   │   ├── user.js             # 用户接口
│   │   ├── match.js            # 匹配接口
│   │   ├── dynamic.js          # 动态接口
│   │   └── chat.js             # 聊天接口
│   ├── assets/styles/          # 全局样式
│   ├── components/             # 通用组件
│   │   ├── UserCard.vue        # 用户卡片
│   │   ├── DynamicCard.vue     # 动态卡片
│   │   └── CommentItem.vue     # 评论项
│   ├── layouts/                # 布局组件
│   │   └── MainLayout.vue     # 主布局
│   ├── router/index.js         # 路由配置
│   ├── stores/user.js          # 用户状态
│   ├── utils/format.js         # 工具函数
│   └── views/                  # 页面
│       ├── auth/               # 登录/注册
│       ├── home/               # 首页
│       ├── user/               # 用户主页
│       ├── dynamic/            # 动态
│       └── chat/               # 聊天
├── vite.config.js
└── package.json
```

---

## 功能页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录页 | /login | 粉紫渐变背景，手机号+密码登录 |
| 注册页 | /register | 手机号/昵称/性别/密码注册 |
| 首页 | /home | 推荐用户卡片，支持筛选 |
| 动态广场 | /dynamic | 动态列表，点赞评论 |
| 发布动态 | /dynamic/publish | 文字+多图 |
| 动态详情 | /dynamic/:id | 动态详情+评论区 |
| 聊天列表 | /chat | 会话列表+未读红点 |
| 聊天室 | /chat/:sessionId | 实时聊天（粉/白气泡） |
| 我的主页 | /profile | 个人资料+动态 |
| 编辑资料 | /profile/edit | 头像/昵称/城市等 |
| 匹配列表 | /match | 互关/喜欢/被喜欢 |

---

## API 接口文档

启动后端后，访问 Swagger 接口文档：
**http://localhost:8080/api/swagger-ui.html**

### 核心接口列表

#### 认证模块（/api/auth）

| 方法 | 路径 | 说明 | 是否需要登录 |
|------|------|------|------|
| POST | /api/auth/register | 注册 | 否 |
| POST | /api/auth/login | 登录 | 否 |
| POST | /api/auth/logout | 退出 | 是 |

#### 用户模块（/api/user）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/me | 获取我的信息 |
| PUT | /api/user/me | 修改我的资料 |
| GET | /api/user/{id} | 查看用户详情 |
| GET | /api/user/list | 用户列表（分页+筛选） |

#### 匹配模块（/api/match）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/match/like/{userId} | 喜欢用户 |
| DELETE | /api/match/unlike/{userId} | 取消喜欢 |
| GET | /api/match/status/{userId} | 获取匹配状态 |
| GET | /api/match/my-likes | 我喜欢的列表 |
| GET | /api/match/who-likes-me | 喜欢我的列表 |
| GET | /api/match/mutual | 互关列表 |

#### 动态模块（/api/dynamic）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/dynamic | 发布动态 |
| DELETE | /api/dynamic/{id} | 删除动态 |
| GET | /api/dynamic/list | 动态列表（分页） |
| GET | /api/dynamic/{id} | 动态详情 |
| GET | /api/dynamic/user/{userId} | 某用户动态 |
| GET | /api/dynamic/my | 我的动态 |

#### 评论模块（/api/comment）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/comment | 添加评论 |
| DELETE | /api/comment/{id} | 删除评论 |
| GET | /api/comment/list/{dynamicId} | 评论列表 |

#### 点赞模块（/api/like）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/like/toggle/{dynamicId} | 切换点赞 |
| GET | /api/like/check/{dynamicId} | 检查是否已赞 |

#### 聊天模块（/api/chat）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/chat/sessions | 会话列表 |
| GET | /api/chat/messages/{sessionId} | 历史消息 |
| POST | /api/chat/send | 发送消息 |
| PUT | /api/chat/read/{sessionId} | 标记已读 |
| GET | /api/chat/unread-count | 未读消息数 |

---

## 常见问题

### Q1: 后端启动报错 "Communications link failure"

**原因：** 连不上 MySQL 数据库。

**解决方法：**
1. 检查 MySQL 服务是否启动：`Get-Service MySQL80`
2. 检查 `application.yml` 中的数据库密码是否正确（Zhang20.）
3. 确保 MySQL 端口 3306 没有被占用

### Q2: 后端启动报错 "Address already in use"

**原因：** 端口 8080 被占用。

**解决方法：**
```powershell
# 查看谁占用了 8080 端口
netstat -ano | findstr :8080
# 根据 PID 结束进程
taskkill /PID 进程号 /F
```

### Q3: 前端请求接口报错 401 或 Network Error

**原因：** 跨域或 token 问题。

**解决方法：**
1. 确保后端已经启动（访问 http://localhost:8080/api/actuator/health 验证）
2. 检查 Vite 代理配置（`vite.config.js` 中的 proxy 是否配置正确）
3. 清除浏览器 localStorage 中的 token，重新登录

### Q4: 前端启动报错 "Module not found"

**原因：** 依赖没有安装完整。

**解决方法：**
```powershell
cd E:\TraePorject\make-friends\make-friends-frontend
npm install
```

### Q5: 启动后页面空白或路由无法访问

**原因：** 路由配置问题。

**解决方法：**
1. 确保访问的是 `http://localhost:5173/`（不是 8080）
2. 打开浏览器 F12 控制台，查看是否有报错信息
3. 确认是否已登录（未登录会自动跳转到 /login）

### Q6: 登录后提示"该手机号已注册"

**原因：** 注册时手机号已存在。

**解决方法：** 换一个手机号注册，或者直接用测试账号登录。

### Q7: 如何修改后端端口？

编辑 `application.yml`：
```yaml
server:
  port: 8080  # 改成你想要的端口
```

---

## 一键启动脚本

每次启动项目都要输入一堆命令很麻烦？创建一个 PowerShell 启动脚本 `start.ps1`：

```powershell
# start.ps1 - 一键启动交友网站

Write-Host "=== 启动 MakeFriends 交友平台 ===" -ForegroundColor Cyan

# 检查 MySQL 服务
$mysql = Get-Service MySQL80 -ErrorAction SilentlyContinue
if ($mysql.Status -ne "Running") {
    Write-Host "正在启动 MySQL 服务..." -ForegroundColor Yellow
    Start-Service MySQL80
    Start-Sleep 2
}
Write-Host "✅ MySQL 服务已启动" -ForegroundColor Green

# 设置环境变量
$env:JAVA_HOME = "D:\JDK"
$env:MAVEN_HOME = "D:\apache-maven-3.9.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

# 启动后端（新窗口）
$backDir = "E:\TraePorject\make-friends\make-friends-backend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backDir'; `$env:JAVA_HOME='D:\JDK'; `$env:MAVEN_HOME='D:\apache-maven-3.9.14'; `$env:PATH=`"`$env:JAVA_HOME\bin;`$env:MAVEN_HOME\bin;`$env:PATH`"; mvn spring-boot:run"
Write-Host "✅ 后端正在启动..." -ForegroundColor Green

Start-Sleep 5

# 启动前端（新窗口）
$frontDir = "E:\TraePorject\make-friends\make-friends-frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontDir'; npm run dev"
Write-Host "✅ 前端正在启动..." -ForegroundColor Green

Start-Sleep 3
Write-Host "`n=== 访问地址 ===" -ForegroundColor Cyan
Write-Host "前端: http://localhost:5173/" -ForegroundColor Yellow
Write-Host "后端: http://localhost:8080/api/" -ForegroundColor Yellow
Write-Host "Swagger文档: http://localhost:8080/api/swagger-ui.html" -ForegroundColor Yellow
Write-Host "`n测试账号: 13800000001 / Test123456" -ForegroundColor Magenta
```

---

## 生产部署

如需部署到云服务器，请参考 [项目计划书](../../JavaWeb交友网站项目计划书.md) 第十一章。

---

## 许可证

本项目仅供学习交流使用。