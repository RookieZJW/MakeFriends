# 🚀 搭伴 (rokbj.me) Windows Server 部署指南

> **适用环境**：阿里云 Windows Server 2019/2022
> **服务器**：2核2G 公网IP：47.114.77.7

---

## 📋 部署架构总览

```
用户浏览器
    │
    ▼
域名 rokbj.me (47.114.77.7)
    │
    ▼
┌─────────────────────────────────┐
│   Nginx (80端口)               │
│   (Windows版)                   │
│                                 │
│   /           → 前端静态文件    │
│   /api/**     → 后端 (8080)     │
│   /ws/**      → 后端WebSocket   │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   Spring Boot 后端 (8080)       │
│   (注册为 Windows 服务)         │
│   ├── MySQL (3306)             │
│   └── Redis (6379)             │
└─────────────────────────────────┘
```

---

## 第一步：域名解析（5 分钟）

### 1.1 进入阿里云域名控制台

1. 登录阿里云控制台：https://dc.console.aliyun.com/
2. 左侧菜单 → **域名与网站** → **域名**
3. 找到 `rokbj.me` → 点击 **解析**

### 1.2 添加解析记录

| 记录类型 | 主机记录 | 记录值 | TTL |
|---------|---------|--------|-----|
| A | @ | 47.114.77.7 | 10分钟 |
| A | www | 47.114.77.7 | 10分钟 |

> 等待 5-30 分钟生效

---

## 第二步：Windows 防火墙开放端口

### 2.1 开放 80 端口（HTTP 网页）

在 Windows 服务器上，用**管理员身份**打开 PowerShell：

```powershell
# 开放 80 端口 (HTTP)
New-NetFirewallRule -DisplayName "HTTP" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow

# 开放 443 端口 (HTTPS)
New-NetFirewallRule -DisplayName "HTTPS" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow

# 开放 22 端口 (SSH，可选，如果你需要远程命令行)
New-NetFirewallRule -DisplayName "SSH" -Direction Inbound -Protocol TCP -LocalPort 22 -Action Allow

# 开放 8080 端口 (后端调试用，生产环境可以不开)
New-NetFirewallRule -DisplayName "Backend" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

或者通过 GUI 操作：
1. `Win + R` → 输入 `wf.msc` → 回车
2. 左侧 → **入站规则** → 右侧 → **新建规则**
3. 选择 **端口** → TCP → 特定端口 `80` → 允许连接 → 全选 → 命名为"HTTP" → 完成

### 2.2 检查阿里云安全组

和防火墙**不一样**的一层，也要开放：

阿里云控制台 → 实例 → 安全组 → 入方向规则：

| 协议 | 端口 | 授权对象 |
|------|------|---------|
| TCP | 22 | 0.0.0.0/0 |
| TCP | 80 | 0.0.0.0/0 |
| TCP | 443 | 0.0.0.0/0 |

---

## 第三步：下载并安装环境

### 3.1 准备工具

推荐用 **Chrome 或 Edge 浏览器** 在服务器上下载：

### 3.2 安装 JDK 17

1. 访问：https://www.oracle.com/java/technologies/downloads/#jdk17-windows
2. 下载 **Windows x64 MSI Installer**（`.msi` 文件）
3. 双击安装 → 一直点"下一步"直到完成
4. **验证安装**：在 cmd 或 PowerShell 执行：
```powershell
java -version
# 应该输出：java version "17.x.x"
```

### 3.3 安装 Redis

Windows 版 Redis 下载地址：

**推荐方式 1：GitHub Releases**
1. 访问：https://github.com/tporadowski/redis/releases
2. 下载最新的 `.msi` 安装包
3. 双击安装 → 全部默认 → 完成
4. 设置密码（可选）：
   - 找到 Redis 安装目录下的 `redis.windows.conf`（通常在 `C:\Program Files\Redis\`）
   - 用记事本打开，搜索 `# requirepass`，改成 `requirepass 你的Redis密码`
5. **验证**：
```powershell
redis-cli -a 你的Redis密码 ping
# 返回 PONG ✅
```

**推荐方式 2：用 Chocolatey 包管理器（如果已安装）**
```powershell
# 安装 Chocolatey（管理员 PowerShell）
Set-ExecutionPolicy Bypass -Scope Process -Force
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 安装 Redis
choco install redis-64 -y
```

### 3.4 安装 MySQL

1. 访问：https://dev.mysql.com/downloads/installer/
2. 下载 **MySQL Installer for Windows**（`mysql-installer-web-community` 或 `mysql-installer-community`）
3. 双击安装：
   - Setup Type 选 **Custom** 或 **Full**
   - 安装 MySQL Server
4. 配置：
   - 设置 root 密码（**记下来！**）
   - 端口保持默认 3306
   - 勾选 **Start the MySQL Server at System Startup**（开机自启）
5. **导入数据库**：
   - 用 Workbench 或命令行：
   ```powershell
   # 打开 cmd
   mysql -u root -p
   ```
   - 在 MySQL 命令行里执行：
   ```sql
   CREATE DATABASE make_friends DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE make_friends;
   -- 导入建表SQL（需要先把SQL文件复制到服务器）
   -- 可以用记事本打开SQL文件，全选复制，粘贴到MySQL命令行执行
   ```

### 3.5 安装 Nginx

1. 访问：https://nginx.org/en/download.html
2. 下载 **Windows 版本**（`nginx-1.xx.x.zip`）
3. 解压到 `C:\nginx\`（建议路径，不要有空格和中文）
4. 修改 Nginx 配置：
   - 用记事本打开 `C:\nginx\conf\nginx.conf`
   - **清空内容**，粘贴下面的完整配置（在后面第七步）
5. 测试 Nginx：
   ```powershell
   cd C:\nginx
   .\nginx.exe -t
   # 显示 test is successful ✅
   ```
6. 启动 Nginx：
   ```powershell
   .\nginx.exe
   ```
7. **验证**：浏览器访问 `http://localhost`，看到"Welcome to nginx" ✅

### 3.6 （可选）设置 SSH 服务

如果需要远程命令行：

1. 打开 PowerShell（管理员）：
```powershell
# 安装 OpenSSH Server
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 启动并设为开机自启
Start-Service sshd
Set-Service -Name sshd -StartupType Automatic

# 验证
Get-Service sshd
# Status : Running ✅
```

---

## 第四步：本地打包项目

### 4.1 打包后端

在你本地 Windows（不是服务器！）执行：
```powershell
cd e:\TraePorject\make-friends\make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests
```

打包后，在 `target\` 目录下有：
```
make-friends-backend-1.0.0.jar
```

### 4.2 打包前端

```powershell
cd e:\TraePorject\make-friends\make-friends-frontend
npm install
npm run build
```

打包后有 `dist\` 目录。

---

## 第五步：上传文件到服务器

### 方式 1：Workbench 直接拖拽（最简单）

1. 打开 Workbench 远程连接
2. 把本地文件**直接拖拽**到 Workbench 的文件管理器里
3. 或者在 Workbench 里打开文件管理器，复制粘贴

### 方式 2：远程桌面复制粘贴

1. 本地 Windows → `Win + R` → 输入 `mstsc`
2. 连接到 `47.114.77.7`
3. 在远程桌面里打开文件管理器
4. 本地复制 → 远程桌面粘贴

### 方式 3：SCP 命令行上传

如果你开启了 SSH：
```powershell
# 上传后端 jar
scp e:\TraePorject\make-friends\make-friends-backend\target\make-friends-backend-1.0.0.jar Administrator@47.114.77.7:C:/rokbj/backend/

# 上传前端 dist
scp -r e:\TraePorject\make-friends\make-friends-frontend\dist\* Administrator@47.114.77.7:C:/rokbj/frontend/dist/
```

### 推荐目录结构

在服务器上创建：
```
C:\rokbj\
    ├── backend\
    │   └── make-friends-backend-1.0.0.jar
    │   └── application-prod.yml
    ├── frontend\
    │   └── dist\          (前端打包文件)
    ├── uploads\           (用户上传的文件)
    └── logs\              (日志)
```

在服务器上用管理员权限创建：
```powershell
mkdir C:\rokbj\backend
mkdir C:\rokbj\frontend
mkdir C:\rokbj\frontend\dist
mkdir C:\rokbj\uploads
mkdir C:\rokbj\logs
```

---

## 第六步：部署后端

### 6.1 修改生产配置

把 `application-prod.yml` 复制到服务器 `C:\rokbj\backend\` 目录

用记事本打开，修改以下内容：

```yaml
spring:
  datasource:
    password: 你的MySQL密码    # ⚠️ 改成你的MySQL密码
  
  data:
    redis:
      password: 你的Redis密码   # ⚠️ 改成你的Redis密码

file:
  upload:
    path: C:/rokbj/uploads/     # ⚠️ Windows路径用 / 或 \\
```

### 6.2 手动启动测试

在服务器 PowerShell（管理员）里执行：
```powershell
cd C:\rokbj\backend

java -Xms256m -Xmx512m -jar make-friends-backend-1.0.0.jar --spring.profiles.active=prod
```

看到 `Started MakeFriendsApplication` 就启动成功了 ✅

**测试接口**（在服务器 cmd 里）：
```powershell
curl http://localhost:8080/api/actuator/health
# 返回 {"status":"UP"} ✅
```

**按 Ctrl+C 停止**，继续下一步。

### 6.3 注册为 Windows 服务（开机自启）

用 **NSSM（Non-Sucking Service Manager）**：

1. 下载 NSSM：https://nssm.cc/download
2. 解压 `nssm-2.24.zip`，把 `nssm-2.24\win64\nssm.exe` 复制到 `C:\Windows\System32\`
3. 管理员 PowerShell 执行：

```powershell
# 安装服务
nssm install RokBJBackend

# 弹出的 GUI 里填写：
# Application 选项卡：
#   Path: C:\Program Files\Java\jdk-17\bin\java.exe  (你的JDK路径)
#   Startup directory: C:\rokbj\backend
#   Arguments: -Xms256m -Xmx512m -jar make-friends-backend-1.0.0.jar --spring.profiles.active=prod
#
# Details 选项卡：
#   Display name: RokBJ Backend Service
#   Description: 搭伴后端服务 (Spring Boot)
#
# I/O 选项卡：
#   Output (stdout): C:\rokbj\logs\backend.log
#   Error (stderr): C:\rokbj\logs\backend-error.log
```

4. 启动服务：
```powershell
nssm start RokBJBackend
```

5. 查看状态：
```powershell
nssm status RokBJBackend
# 显示 SERVICE_RUNNING ✅
```

6. 设置自动重启（服务崩溃后自动恢复）：
```powershell
nssm edit RokBJBackend
# 切换到 "I/O" 选项卡旁边的 "Actions"
# Application restart: 勾选 Restart delays 5000 milliseconds
```

**常用 NSSM 命令**：
```powershell
nssm status RokBJBackend    # 查看状态
nssm stop RokBJBackend      # 停止
nssm restart RokBJBackend   # 重启
nssm edit RokBJBackend      # 修改配置
nssm remove RokBJBackend    # 删除服务
```

---

## 第七步：配置 Nginx

### 7.1 修改 Nginx 配置

用记事本打开 `C:\nginx\conf\nginx.conf`，**清空内容**，粘贴：

```nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    # 开启 gzip 压缩
    gzip on;
    gzip_comp_level 6;
    gzip_min_length 1024;
    gzip_vary on;
    gzip_types
        text/plain text/css text/javascript application/json
        application/javascript application/xml+rss application/rss+xml
        application/atom+xml image/svg+xml;

    # 服务器定义
    server {
        listen       80;
        server_name  rokbj.me www.rokbj.me;

        # 前端静态文件根目录
        root   C:/rokbj/frontend/dist;
        index  index.html;

        # ========== WebSocket 支持 ==========
        map $http_upgrade $connection_upgrade {
            default upgrade;
            ''      close;
        }

        # ========== 后端 API 代理 ==========
        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 30s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # ========== WebSocket 代理 ==========
        location /ws/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_read_timeout 86400s;
        }

        # ========== Vue Router 历史模式 ==========
        location / {
            try_files $uri $uri/ /index.html;
        }

        # ========== 静态资源缓存 ==========
        location ~* \.(?:css|js|jpg|jpeg|png|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 7d;
            add_header Cache-Control "public, immutable";
            access_log off;
        }

        # ========== 安全头 ==========
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
    }
}
```

### 7.2 测试并重载 Nginx

```powershell
cd C:\nginx

# 测试配置
.\nginx.exe -t
# 显示 test is successful ✅

# 如果 Nginx 已经在运行，先停止再启动
.\nginx.exe -s stop
Start-Sleep -Seconds 2
.\nginx.exe

# 或者直接重载配置
.\nginx.exe -s reload
```

### 7.3 验证

浏览器访问 `http://localhost` 或 `http://rokbj.me`，能看到搭伴的登录页 ✅

---

## 第八步：验证部署

### 8.1 测试清单

| 测试项 | URL | 预期结果 |
|--------|-----|---------|
| 前端首页 | http://rokbj.me | 显示登录页 |
| API 健康检查 | http://rokbj.me/api/actuator/health | `{"status":"UP"}` |
| 登录功能 | http://rokbj.me | 能正常登录 |
| 聊天功能 | 登录后 → 消息 | 能收发消息 |
| WebSocket | 聊天页面 | 实时消息正常 |
| 文件上传 | 编辑资料 → 上传头像 | 上传成功 |

### 8.2 查看日志

```powershell
# 后端日志
Get-Content C:\rokbj\logs\backend.log -Tail 100

# Nginx 日志
Get-Content C:\nginx\logs\access.log -Tail 10
Get-Content C:\nginx\logs\error.log -Tail 10
```

---

## 第九步：将 Nginx 注册为 Windows 服务（可选）

让 Nginx 开机自动启动：

```powershell
# 下载 Nginx 服务包装器
# 或用 nssm：
nssm install RokBJNginx

# Application:
#   Path: C:\nginx\nginx.exe
#   Startup directory: C:\nginx
#   Arguments: -g "nginx.conf"
#
# Details:
#   Display name: RokBJ Nginx Service

nssm start RokBJNginx
```

或者更简单，用 **任务计划程序**：
1. `Win + R` → `taskschd.msc`
2. 创建基本任务 → 触发器：计算机启动时
3. 操作：启动程序 → `C:\nginx\nginx.exe`
4. 勾选"不管用户是否登录都运行"

---

## 🎉 恭喜！部署完成！

现在任何人都可以通过 `http://rokbj.me` 访问你的"搭伴"网站了！

---

## 📝 常用运维命令

```powershell
# 查看后端状态
nssm status RokBJBackend

# 重启后端（更新代码后）
nssm stop RokBJBackend
nssm start RokBJBackend

# 重启 Nginx
cd C:\nginx
.\nginx.exe -s reload    # 重载配置
.\nginx.exe -s stop      # 停止
.\nginx.exe              # 启动

# 查看后端日志
Get-Content C:\rokbj\logs\backend.log -Tail 100 -Wait

# 查看服务器资源
# 任务管理器 → 性能 → CPU/内存
```

---

## 🔄 以后更新代码

1. **本地重新打包**：
```powershell
# 后端
cd e:\TraePorject\make-friends\make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests

# 前端
cd e:\TraePorject\make-friends\make-friends-frontend
npm run build
```

2. **上传到服务器**（通过远程桌面复制粘贴）：
   - `make-friends-backend-1.0.0.jar` → `C:\rokbj\backend\`
   - `dist\` 所有文件 → `C:\rokbj\frontend\dist\`

3. **重启服务**：
```powershell
nssm restart RokBJBackend
cd C:\nginx && .\nginx.exe -s reload
```

---

## ⚠️ 常见问题

### Q: 访问域名显示 502 Bad Gateway
**原因**：后端没有运行
**解决**：`nssm status RokBJBackend` 检查状态

### Q: 页面空白
**原因**：前端文件路径不对
**解决**：检查 Nginx 配置中的 `root` 路径

### Q: API 请求 404
**原因**：Nginx 代理配置问题
**解决**：检查 `location /api/` 和 `proxy_pass` 配置

### Q: WebSocket 连接失败
**原因**：Nginx 缺少配置
**解决**：检查 `/ws/` location 块

### Q: 上传文件失败
**原因**：上传目录权限
**解决**：给 `C:\rokbj\uploads\` 设置 Everyone 写入权限
```powershell
icacls C:\rokbj\uploads /grant Everyone:(OI)(CI)M
```

---

## 🔒 安全建议

1. **配置 SSL 证书**（HTTPS）：
   - 阿里云有免费的 SSL 证书（Symantec DV）
   - 申请后下载 IIS 版本的证书
   - 在 Nginx 配置中加入 SSL 配置

2. **设置 Redis 密码**（重要！）

3. **设置 MySQL 密码**（重要！）

4. **定期备份**：
```powershell
# 导出数据库备份
cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"
mysqldump -u root -p make_friends > C:\rokbj\backup.sql
```

---

祝部署顺利！🎉🎉🎉
