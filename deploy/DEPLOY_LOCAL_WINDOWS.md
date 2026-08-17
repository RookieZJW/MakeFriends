# 🚀 搭伴 (rokbj.me) 本地电脑部署完整手册

> **方案**：本地 Windows 电脑当服务器 + Cloudflare Tunnel 内网穿透 + 阿里云域名
> **适用场景**：个人开发 / 测试 / 小流量演示（正式生产建议用云服务器）
> **费用**：完全免费（只需要付域名年费）
> **最后更新**：2026-08-17

---

## 📋 架构总览

```
┌──────────────────────────────────────────────────────────┐
│                     任意用户浏览器                         │
│                  访问 https://rokbj.me                    │
└──────────────────────────────────────┬───────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────┐
│             Cloudflare (全球 CDN + DNS + 防火墙)          │
│   自动提供 HTTPS、DDoS 防护、缓存加速、IP 访问加速         │
└──────────────────────────────────────┬───────────────────┘
                                       │ 加密隧道 (出站连接)
                                       ▼
┌──────────────────────────────────────────────────────────┐
│              你的 Windows 本地电脑 (在家中)                │
│                                                          │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐ │
│  │ cloudflared   │→ │   Nginx :80   │→ │ 前端 dist 静态 │ │
│  │  (Tunnel 客户 │  │  (反向代理)   │  │  文件 (Vue)    │ │
│  │   端)         │  └───────┬───────┘  └───────────────┘ │
│  └───────────────┘          │                            │
│                             │ /api/, /ws/                │
│                             ▼                            │
│                  ┌──────────────────────┐                │
│                  │ Spring Boot :8080    │                │
│                  │  (后端 Java 服务)     │                │
│                  └──────┬────────┬──────┘                │
│                         │        │                        │
│                         ▼        ▼                        │
│                  ┌──────────┐ ┌──────────┐                │
│                  │ MySQL    │ │ Redis    │                │
│                  │ :3306    │ │ :6379    │                │
│                  └──────────┘ └──────────┘                │
└──────────────────────────────────────────────────────────┘
```

**核心原理**：cloudflared 在你电脑上主动向 Cloudflare 建立出站加密隧道，所有外部请求通过 Cloudflare → 隧道 → 你电脑。因为是**出站连接**，所以：
- ✅ 不需要公网 IP
- ✅ 不需要路由器/光猫端口转发
- ✅ 不需要开放 Windows 防火墙入站端口
- ✅ 自动获得 HTTPS（Cloudflare 免费证书）

---

## 💻 你的环境信息（已部署成功，存档用）

| 项目 | 值 | 备注 |
|------|---|------|
| 域名 | `rokbj.me` | 阿里云购买 |
| DNS 托管 | Cloudflare | 已从阿里云 DNS 迁移 |
| Tunnel ID | `557754ad-8013-4d31-97f0-76e679cf3e76` | 名称：rokbj |
| Cloudflare DNS 服务器 | `cris.ns.cloudflare.com` / `fatima.ns.cloudflare.com` | |
| 本地电脑 IP | IPv6 公网 / IPv4 内网 | 用 Tunnel 无需关注 |
| JDK 路径 | `D:\JDK` | JDK 17 |
| Maven 路径 | `D:\apache-maven-3.9.14` | |
| Redis 路径 | `D:\Redis-8.6.4` | |
| Nginx 路径 | `D:\nginx` | 端口：80 |
| 后端 Jar 包 | `make-friends-backend/target/make-friends-backend-1.0.0.jar` | 端口：8080 |
| 前端 dist 路径 | `make-friends-frontend/dist/` | |
| cloudflared 配置 | `C:\Users\25803\.cloudflared\config.yml` | |
| cloudflared 凭证 | `C:\Users\25803\.cloudflared\cert.pem` | |
| Tunnel 凭证 | `C:\Users\25803\.cloudflared\557754ad-8013-4d31-97f0-76e679cf3e76.json` | ⚠️ 不要泄露 |

---

## 📦 目录结构（部署相关文件）

```
make-friends/
├── deploy/                           # ⭐ 部署文档和配置（把本目录的文件都复制到这里）
│   ├── DEPLOY_LOCAL_WINDOWS.md       # 本文件：完整部署手册
│   ├── nginx.conf                    # Nginx 配置（和 D:\nginx\conf\nginx.conf 一致）
│   ├── cloudflared-config.yml        # Cloudflare Tunnel 配置模板
│   ├── start-all.ps1                 # ✨ 一键启动所有服务（推荐）
│   ├── stop-all.ps1                  # ✨ 一键停止所有服务
│   ├── status-all.ps1                # ✨ 查看所有服务状态
│   ├── DEPLOY_GUIDE_CLOUD.md         # 阿里云服务器方案（备选）
│   └── DEPLOY_GUIDE_WINDOWS.md       # Windows Server 云服务器方案（备选）
├── make-friends-backend/
│   └── target/
│       └── make-friends-backend-1.0.0.jar  # 后端打包产物
└── make-friends-frontend/
    └── dist/                         # 前端打包产物
        └── index.html
```

---

# ⚡ 快速启动（日常使用，推荐）

假设所有环境都已经装好了（就是你现在的情况），以后每次开机只需要：

## 方法一：一键脚本（强烈推荐）

打开 PowerShell，进入 `make-friends\deploy` 目录：

```powershell
cd E:\TraePorject\make-friends\deploy

# 一键启动所有服务
.\start-all.ps1

# 查看服务状态
.\status-all.ps1

# 一键停止所有服务
.\stop-all.ps1
```

脚本会依次启动：Redis → MySQL → 后端 → Nginx → Cloudflare Tunnel，并显示每个服务的状态。

---

## 方法二：手动启动（按顺序）

如果你想了解每个步骤，手动启动：

### Step 1：启动 Redis

```powershell
cd D:\Redis-8.6.4
.\redis-server.exe
```
验证：新开 cmd → `redis-cli ping` → 返回 `PONG` ✅

### Step 2：确认 MySQL 在运行

任务管理器 → 服务 → 找到 MySQL → 状态「正在运行」
如果没启动 → 右键 → 启动

### Step 3：启动后端 Spring Boot

```powershell
cd E:\TraePorject\make-friends\make-friends-backend
java -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar
```
验证：看到 `Started MakeFriendsApplication` 日志 ✅

### Step 4：启动 Nginx

```powershell
cd D:\nginx
.\nginx.exe
```
验证：`tasklist | findstr nginx` 有进程 ✅
浏览器访问 `http://localhost` → 看到搭伴页面 ✅

### Step 5：启动 Cloudflare Tunnel

```powershell
cloudflared tunnel run rokbj
```
验证：看到 `Tunnel started` + `Connection 1 registered` 日志 ✅

### 最终验证

用手机 4G/5G（别连你家 WiFi）访问：
```
https://rokbj.me
```
看到搭伴登录页 → 🎉 **成功！**

---

# 🛠️ 从零开始完整部署流程（新电脑需要看这里）

如果以后换电脑，或者朋友想搭同样的环境，按这个流程一步一步来。

---

## 第一阶段：准备工作（约 10 分钟）

### 1.1 硬件和网络要求

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 4 核 | 6 核及以上 |
| 内存 | 8 GB | 16 GB 及以上 |
| 硬盘 | 50 GB 可用 | SSD 200 GB+ |
| 宽带 | 10 Mbps 上传 | 50 Mbps+ 上传 |
| 电脑 | 不能关机/休眠 | 设置永不休眠 + 接通电源 |

⚠️ **重要设置**：
- Windows 设置 → 系统 → 电源和睡眠 → 「睡眠」改为「从不」
- 如果是笔记本：电池选项 → 高级设置 → 「合上盖子操作」改为「不采取任何操作」

### 1.2 软件清单（都要先装好）

| 软件 | 版本 | 下载地址 | 用途 |
|------|------|---------|------|
| JDK | 17 | Oracle 官网 / Adoptium | 运行后端 Java |
| Maven | 3.9+ | https://maven.apache.org/ | 打包后端 |
| Node.js | 18+ | https://nodejs.org/ | 打包前端 |
| MySQL | 8.0 | https://dev.mysql.com/downloads/mysql/ | 数据库 |
| Redis | 7.0+ | https://github.com/tporadowski/redis/releases | 缓存/Session |
| Nginx for Windows | 1.26+ | https://nginx.org/en/download.html | 反向代理 |
| cloudflared | 最新版 | https://github.com/cloudflare/cloudflared/releases | 内网穿透 |
| Git | 最新版 | https://git-scm.com/ | 代码管理（可选） |

---

## 第二阶段：域名和 Cloudflare 配置（约 15 分钟）

### 2.1 注册 Cloudflare 账号

1. 访问 https://dash.cloudflare.com/sign-up
2. 邮箱 + 密码注册（免费账号即可）

### 2.2 添加域名到 Cloudflare

1. 登录后 → **Add a Site**
2. 输入 `rokbj.me` → Continue
3. 选择 **Free** 计划 → Continue
4. Cloudflare 会扫描现有 DNS 记录 → 直接 Continue

### 2.3 修改阿里云域名的 DNS 服务器

1. Cloudflare 页面会给你分配 2 个 DNS 服务器，比如：
   ```
   cris.ns.cloudflare.com
   fatima.ns.cloudflare.com
   ```
   ⚠️ 以你的 Cloudflare 页面显示的为准，**不要直接抄上面的**！

2. 登录阿里云域名控制台 → https://dc.console.aliyun.com/
3. 找到 `rokbj.me` → 管理 → DNS 服务器 → 修改 DNS
4. 删除原有的阿里云 DNS（比如 `dns17.hichina.com`）
5. 添加 Cloudflare 给你的 2 个新 DNS 服务器
6. 保存，等待生效（5-30 分钟，Cloudflare 会发邮件通知）

### 2.4 创建 Cloudflare Tunnel

1. **安装 cloudflared**：下载 Windows MSI 安装包，双击安装
2. **验证安装**：
   ```powershell
   cloudflared --version
   ```
3. **登录 Cloudflare**：
   ```powershell
   cloudflared tunnel login
   ```
   → 自动打开浏览器 → 选择 `rokbj.me` → Authorize
4. **创建隧道**：
   ```powershell
   cloudflared tunnel create rokbj
   ```
   → 成功输出 Tunnel ID，把它记下来
   ```
   Created tunnel rokbj with id xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```
5. **创建配置文件**：
   在 `C:\Users\你的用户名\.cloudflared\` 下创建 `config.yml`：
   ```yaml
   tunnel: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  # 你的 Tunnel ID
   credentials-file: C:\Users\你的用户名\.cloudflared\xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json

   ingress:
     # 后端 API
     - hostname: rokbj.me
       path: /api/.*
       service: http://localhost:8080

     # WebSocket
     - hostname: rokbj.me
       path: /ws/.*
       service: http://localhost:8080

     # 前端
     - hostname: rokbj.me
       service: http://localhost:80

     # www 子域名
     - hostname: www.rokbj.me
       service: http://localhost:80

     # 兜底
     - service: http_status:404
   ```
6. **验证配置**：
   ```powershell
   cloudflared tunnel ingress validate
   # 输出 Validated rules successfully ✅
   ```
7. **配置 DNS 路由**：
   ```powershell
   cloudflared tunnel route dns rokbj rokbj.me
   cloudflared tunnel route dns rokbj www.rokbj.me
   ```
   如果报错 `record already exists`，去 Cloudflare DNS 页面把旧的 A/AAAA 记录删掉，或者加 `--overwrite-dns` 参数。

---

## 第三阶段：本地环境配置（约 20 分钟）

### 3.1 MySQL 配置

1. 确保 MySQL 服务在运行
2. 创建数据库 `makefriends`：
   ```sql
   CREATE DATABASE makefriends DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
3. 导入表结构：
   ```powershell
   mysql -u root -p makefriends < E:\TraePorject\make-friends\make-friends-backend\sql\makefriends_schema.sql
   ```
4. 然后导入索引：
   ```powershell
   mysql -u root -p makefriends < E:\TraePorject\make-friends\make-friends-backend\sql\004_db_indexes.sql
   ```

### 3.2 Redis 配置

1. 启动 Redis：
   ```powershell
   cd D:\Redis-8.6.4
   .\redis-server.exe
   ```
2. （建议）设置 Redis 密码，编辑 `redis.conf`：
   ```conf
   requirepass 你的强密码
   ```
   ⚠️ 同时要改 `application.yml` 里的 Redis 密码配置

### 3.3 Nginx 配置

1. 下载 Nginx Windows zip 包 → 解压到 `D:\nginx`
2. 把项目目录下的 `deploy/nginx.conf` 复制到 `D:\nginx\conf\nginx.conf`
3. **修改 `nginx.conf` 里的 root 路径**（改成你自己的）：
   ```nginx
   root   E:/TraePorject/make-friends/make-friends-frontend/dist;
   ```
   ⚠️ 路径分隔符用 `/`，不要用 Windows 默认的 `\`
4. 验证配置：
   ```powershell
   cd D:\nginx
   .\nginx.exe -t
   # syntax is ok / test is successful ✅
   ```

### 3.4 后端配置（application-prod.yml）

如果后端需要生产环境配置（比如文件上传路径改成绝对路径）：

1. 在 `make-friends-backend/src/main/resources/` 下创建 `application-prod.yml`
2. 配置内容示例：
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
     datasource:
       url: jdbc:mysql://127.0.0.1:3306/makefriends?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
       username: root
       password: 你的MySQL密码

   file:
     upload:
       path: E:/TraePorject/make-friends/uploads/
   ```
3. 启动时指定 profile：
   ```powershell
   java -jar target\make-friends-backend-1.0.0.jar --spring.profiles.active=prod
   ```

---

## 第四阶段：打包项目

### 4.1 打包后端

```powershell
cd E:\TraePorject\make-friends\make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests
```
打包成功后：`target/make-friends-backend-1.0.0.jar` ✅

### 4.2 打包前端

```powershell
cd E:\TraePorject\make-friends\make-friends-frontend
npm run build
```
打包成功后：`dist/` 目录下有 `index.html` ✅

---

## 第五阶段：启动所有服务

参考「快速启动」章节，用一键脚本或者手动启动。

---

# 🔄 日常运维

## 更新代码后重新部署

```powershell
# 1. 拉代码
cd E:\TraePorject\make-friends
git pull

# 2. 打包后端
cd make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests

# 3. 打包前端
cd ..\make-friends-frontend
npm run build

# 4. 重启后端
# 先用 Ctrl+C 停掉旧的后端，再运行：
java -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar

# 5. 重载 Nginx
cd D:\nginx
.\nginx.exe -s reload
```

## 查看服务状态

```powershell
# 查看端口占用
netstat -ano | findstr ":80 :8080 :6379 :3306"

# 查看 Nginx 进程
tasklist | findstr nginx

# 查看 Java 后端进程
tasklist | findstr java

# 查看 cloudflared 进程
tasklist | findstr cloudflared

# 或者用 deploy 目录下的：
.\status-all.ps1
```

## 查看日志

| 服务 | 日志位置 | 查看方式 |
|------|---------|---------|
| 后端 | 启动后端的 cmd 窗口输出 | 直接看，或者配置 `logging.file.path` 输出到文件 |
| Nginx 错误日志 | `D:\nginx\logs\error.log` | `Get-Content D:\nginx\logs\error.log -Tail 50` |
| Nginx 访问日志 | `D:\nginx\logs\access.log` | `Get-Content D:\nginx\logs\access.log -Tail 50` |
| cloudflared | 运行 `cloudflared tunnel run` 的 cmd 窗口 | 直接看 |
| MySQL | Windows 事件查看器 | 事件查看器 → Windows 日志 → 应用程序 |

## 常见问题排查

### ❌ 访问 https://rokbj.me 显示 502 Bad Gateway

Cloudflare Tunnel 连接正常，但你的本地服务没起来：
1. 检查 Nginx 有没有启动 → `tasklist | findstr nginx`
2. 检查后端有没有启动 → 访问 http://localhost:8080
3. 看 Nginx 错误日志

### ❌ 显示 "Tunnel not found" 或 DNS 错误

1. 去 Cloudflare DNS 页面，确认 CNAME 记录存在，目标是 `TunnelID.cfargotunnel.com`
2. 确认 Tunnel 正在运行（`cloudflared tunnel run rokbj` 的 cmd 窗口不能关）
3. 运行 `cloudflared tunnel info rokbj` 查看隧道状态

### ❌ 访问本地 http://localhost 显示 404

1. 确认前端已经打包（`npm run build`）
2. 检查 nginx.conf 里的 `root` 路径是否正确
3. 重载 Nginx：`.\nginx.exe -s reload`

### ❌ 登录时提示接口错误 / 401

1. 检查后端是否启动成功
2. 检查 MySQL 是否运行
3. 检查 Redis 是否运行
4. 看后端日志的具体错误信息

### ❌ 聊天 WebSocket 连不上

1. 确认 nginx.conf 里的 `/ws/` location 配置正确（`Upgrade` 和 `Connection` 头）
2. 确认后端 `ChatWebSocketHandler` 正常
3. 浏览器 F12 → Network → WS，看连接错误信息

### ❌ cloudflared 启动后报错 "no ingress rule matches"

检查 `C:\Users\25803\.cloudflared\config.yml` 里的 ingress 规则，确保最后有 `- service: http_status:404` 作为兜底，并运行 `cloudflared tunnel ingress validate` 验证。

---

# 🔒 设置开机自启（进阶）

如果你希望电脑开机后自动运行所有服务（不用手动点），可以用 NSSM 把它们注册为 Windows 服务。

## 安装 NSSM

下载 NSSM：https://nssm.cc/download → 把 `nssm.exe` 放到 `C:\Windows\System32\`

## 注册后端服务

```powershell
nssm install RokBJBackend
# GUI 弹出：
#   Path: D:\JDK\bin\java.exe
#   Startup directory: E:\TraePorject\make-friends\make-friends-backend
#   Arguments: -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar
nssm start RokBJBackend
```

## 注册 Nginx 服务

```powershell
nssm install RokBJNginx
# GUI:
#   Path: D:\nginx\nginx.exe
#   Startup directory: D:\nginx
nssm start RokBJNginx
```

## 注册 Cloudflare Tunnel 服务

```powershell
cloudflared service install
# 这条命令会自动把 cloudflared 注册为服务
```

## 注册 Redis 服务（如果没注册的话）

```powershell
cd D:\Redis-8.6.4
.\redis-server.exe --service-install redis.conf --service-name Redis
.\redis-server.exe --service-start --service-name Redis
```

## 管理服务

```powershell
# 查看状态
Get-Service RokBJ*
Get-Service cloudflared
Get-Service Redis
Get-Service MySQL*

# 手动启动
Start-Service RokBJBackend
Start-Service RokBJNginx

# 手动停止
Stop-Service RokBJBackend
Stop-Service RokBJNginx
```

---

# 📊 性能和限制

## 本地方案的性能天花板

| 资源 | 家庭宽带典型值 | 影响 |
|------|--------------|------|
| 上传带宽 | 20-50 Mbps | 约支持 50-200 人同时在线（取决于图片/文件大小） |
| 内存 | 8 GB | 跑 MySQL + Redis + 后端 + Nginx + Windows 系统本身，刚够用 |
| CPU | 4 核 | 100 人以内并发 OK |

## 推荐使用场景

- ✅ 个人开发 / 测试
- ✅ 小范围内测（50 人以内）
- ✅ 演示给朋友看
- ❌ 正式生产运营（建议迁移到云服务器）

## 迁移到云服务器

等项目用户量上来，再迁移到阿里云服务器，参考：
- `DEPLOY_GUIDE_CLOUD.md`（阿里云 Linux 服务器方案）
- `DEPLOY_GUIDE_WINDOWS.md`（阿里云 Windows Server 方案）

迁移很简单：
1. 在云服务器上装 JDK + MySQL + Redis + Nginx
2. 导出本地 MySQL 数据 → 导入到云服务器
3. 把 Nginx 配置稍微改改（端口转发用内网地址即可，不需要 Tunnel）
4. 把 Cloudflare Tunnel 切换回普通 A 记录指向云服务器 IP

---

# 🆘 求助

如果遇到问题，按下面顺序排查：

1. 先看本手册的「常见问题排查」章节
2. 查看对应服务的日志（最关键！）
3. 用 `status-all.ps1` 确认每个服务都在运行
4. 先本地验证（http://localhost 能不能打开），再验证外网
5. 本地正常、外网异常 → 一般是 Cloudflare Tunnel 没连好

---

**祝使用愉快！** 🎉
