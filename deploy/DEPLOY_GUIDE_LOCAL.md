# 🚀 搭伴 (rokbj.me) 本地电脑部署指南

> **方案**：本地电脑当服务器 + Cloudflare Tunnel 内网穿透 + 阿里云域名
> **费用**：完全免费
> **适用**：没有公网IP的家庭宽带

---

## 📋 架构总览

```
用户浏览器
    │
    ▼
域名 rokbj.me (Cloudflare DNS)
    │
    ▼
┌─────────────────────────────────┐
│   Cloudflare Tunnel (免费)      │
│   (加密隧道，连接你的电脑)       │
└─────────────────────────────────┘
    │
    ▼ (加密隧道)
┌─────────────────────────────────┐
│   你的电脑                       │
│                                 │
│   Nginx (80端口)                │
│   ├── 前端 dist 静态文件        │
│   ├── /api/ → 后端 (8080)       │
│   └── /ws/  → 后端 WebSocket    │
│                                 │
│   Spring Boot (8080)            │
│   ├── MySQL (3306)             │
│   └── Redis (6379)             │
└─────────────────────────────────┘
```

**原理**：Cloudflare Tunnel 会在你电脑上运行一个小程序，它主动连接 Cloudflare 服务器建立加密隧道。外部用户访问 rokbj.me → Cloudflare → 隧道 → 你的电脑。

---

## 第一步：注册 Cloudflare（5分钟）

### 1.1 注册账号

1. 访问：https://dash.cloudflare.com/sign-up
2. 输入邮箱和密码 → 注册

### 1.2 添加你的域名

1. 登录 Cloudflare → 点击 **Add a Site**
2. 输入 `rokbj.me` → 点击 Continue
3. 选择 **Free** 免费计划
4. Cloudflare 会扫描现有 DNS 记录

### 1.3 修改域名 DNS 服务器

这一步**非常关键**！需要把阿里云域名的 DNS 服务器改成 Cloudflare 的：

1. Cloudflare 会给你两个 DNS 服务器地址，类似：
   ```
   ns1.cloudflare.com
   ns2.cloudflare.com
   ```

2. 登录**阿里云域名控制台**：
   - https://dc.console.aliyun.com/
   - 找到 `rokbj.me` → 点击 **管理**
   - 找到 **DNS 服务器** → 点击 **修改 DNS**
   - 把原来的阿里云 DNS 改成 Cloudflare 给的两个：
     ```
     ns1.cloudflare.com
     ns2.cloudflare.com
     ```

3. 等待 DNS 生效（通常 5-30 分钟）
   - Cloudflare 会发邮件通知你 DNS 已激活

### 1.4 添加 DNS 记录

在 Cloudflare 控制台 → DNS → Records：

| 类型 | 名称 | 内容 | 代理状态 |
|------|------|------|---------|
| A | @ | 127.0.0.1 | 仅DNS（灰色云朵） |
| A | www | 127.0.0.1 | 仅DNS（灰色云朵） |

> 注意：IP 填 127.0.0.1 就行，因为实际流量通过 Tunnel 走，不经过这个 A 记录
> 或者干脆不添加 A 记录，Tunnel 会自动创建 CNAME

---

## 第二步：安装 Cloudflare Tunnel

### 2.1 下载 cloudflared

1. 访问：https://github.com/cloudflare/cloudflared/releases/latest
2. 下载 **Windows 版本**：`cloudflared-windows-amd64.msi`
3. 双击安装 → 一路下一步

### 2.2 验证安装

打开 cmd 或 PowerShell：
```powershell
cloudflared --version
# 输出 cloudflared version 202x.x.x ✅
```

---

## 第三步：登录 Cloudflare 并创建隧道

### 3.1 登录

```powershell
cloudflared tunnel login
```

- 会自动打开浏览器
- 选择 `rokbj.me` 域名 → 点击 Authorize
- 回到终端，显示成功 ✅

### 3.2 创建隧道

```powershell
cloudflared tunnel create rokbj
```

输出类似：
```
Created tunnel rokbj with id xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**记下这个 Tunnel ID**（后面要用）

### 3.3 创建配置文件

在 `C:\Users\你的用户名\.cloudflared\` 目录下创建 `config.yml`：

```yaml
tunnel: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  # 换成你的 Tunnel ID
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

  # 前端（所有其他请求）
  - hostname: rokbj.me
    service: http://localhost:80

  # 兜底
  - service: http_status:404
```

> ⚠️ 把 `你的用户名` 和 Tunnel ID 换成实际的值

### 3.4 配置 DNS

```powershell
cloudflared tunnel route dns rokbj rokbj.me
```

这会自动在 Cloudflare DNS 里创建一条 CNAME 记录指向你的隧道。

---

## 第四步：本地安装环境

### 4.1 安装 JDK 17

你本地应该已经装了（开发用的），跳过。

如果没装：
- 下载：https://www.oracle.com/java/technologies/downloads/#jdk17-windows
- 安装 MSI 版本

验证：
```powershell
java -version
```

### 4.2 安装 Redis

你本地应该已经装了（在 D:\Redis-8.6.4）

验证：
```powershell
redis-cli ping
# 返回 PONG ✅
```

### 4.3 安装 MySQL

你本地应该已经装了。

验证：
```powershell
mysql -u root -p
# 输入密码后能进入 ✅
```

### 4.4 安装 Nginx（Windows版）

1. 下载：https://nginx.org/en/download.html
2. 解压到 `D:\nginx\`（或任意目录，不要有空格中文）
3. 修改配置文件 `D:\nginx\conf\nginx.conf`，清空后粘贴：

```nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    gzip on;
    gzip_comp_level 6;
    gzip_min_length 1024;
    gzip_types
        text/plain text/css text/javascript application/json
        application/javascript application/xml+rss;

    server {
        listen       80;
        server_name  localhost;

        # 前端静态文件
        root   E:/TraePorject/make-friends/make-friends-frontend/dist;
        index  index.html;

        # WebSocket 支持
        map $http_upgrade $connection_upgrade {
            default upgrade;
            ''      close;
        }

        # 后端 API 代理
        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # WebSocket 代理
        location /ws/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_set_header Host $host;
            proxy_read_timeout 86400s;
        }

        # Vue Router 历史模式
        location / {
            try_files $uri $uri/ /index.html;
        }

        # 静态资源缓存
        location ~* \.(?:css|js|jpg|jpeg|png|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 7d;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

4. 测试并启动：
```powershell
cd D:\nginx
.\nginx.exe -t      # 测试配置
.\nginx.exe          # 启动
```

5. 验证：浏览器访问 `http://localhost` → 看到搭伴登录页 ✅

---

## 第五步：打包项目

### 5.1 打包后端

```powershell
cd e:\TraePorject\make-friends\make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests
```

### 5.2 打包前端

```powershell
cd e:\TraePorject\make-friends\make-friends-frontend
npm run build
```

---

## 第六步：启动所有服务

### 6.1 启动后端

```powershell
cd e:\TraePorject\make-friends\make-friends-backend

# 创建生产配置文件
# 复制 application.yml 改名为 application-prod.yml
# 修改 file.upload.path 为本地路径

java -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar --spring.profiles.active=prod
```

### 6.2 启动 Nginx

```powershell
cd D:\nginx
.\nginx.exe
```

### 6.3 启动 Cloudflare Tunnel

```powershell
cloudflared tunnel run rokbj
```

看到类似日志就说明隧道连接成功：
```
INF Connection ... registered
INF Tunnel started
```

### 6.4 验证

打开浏览器访问 `https://rokbj.me`（注意是 https！Cloudflare 自动提供 SSL）

- 看到搭伴登录页 ✅
- 能正常登录 ✅
- 聊天功能正常 ✅

🎉 恭喜！任何人都可以通过 `https://rokbj.me` 访问你的网站了！

---

## 第七步：设置开机自启（重要！）

### 7.1 后端开机自启

用 NSSM 注册为 Windows 服务：

1. 下载 NSSM：https://nssm.cc/download
2. 把 `nssm.exe` 放到 `C:\Windows\System32\`
3. 管理员 PowerShell：

```powershell
nssm install RokBJBackend

# 弹出 GUI：
# Path: D:\JDK\bin\java.exe
# Startup directory: E:\TraePorject\make-friends\make-friends-backend
# Arguments: -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar --spring.profiles.active=prod
```

```powershell
nssm start RokBJBackend
```

### 7.2 Nginx 开机自启

```powershell
nssm install RokBJNginx

# Path: D:\nginx\nginx.exe
# Startup directory: D:\nginx
```

```powershell
nssm start RokBJNginx
```

### 7.3 Cloudflare Tunnel 开机自启

```powershell
cloudflared service install
```

这会把 cloudflared 注册为 Windows 服务，开机自动运行。

---

## 🔄 以后更新代码

1. 本地重新打包：
```powershell
# 后端
cd e:\TraePorject\make-friends\make-friends-backend
D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests

# 前端
cd e:\TraePorject\make-friends\make-friends-frontend
npm run build
```

2. 重启后端：
```powershell
nssm restart RokBJBackend
```

3. 重载 Nginx：
```powershell
cd D:\nginx
.\nginx.exe -s reload
```

---

## ⚠️ 注意事项

1. **电脑不能关机**：关机后网站就访问不了了
2. **电脑不能休眠**：设置 → 电源 → 永不休眠
3. **宽带上传速度**：家庭宽带上传速度有限（通常 20-50Mbps），高并发会卡
4. **Redis 设置密码**：即使在内网也建议设置密码
5. **MySQL 密码**：确保密码足够复杂
6. **防火墙**：Cloudflare Tunnel 不需要开放任何端口，本地防火墙不用改

---

## 📝 常用命令

```powershell
# 查看 Tunnel 状态
cloudflared tunnel info rokbj

# 查看后端日志
nssm status RokBJBackend
Get-Content E:\TraePorject\make-friends\make-friends-backend\logs\backend.log -Tail 100

# 重启所有服务
nssm restart RokBJBackend
cd D:\nginx && .\nginx.exe -s reload

# 停止 Tunnel
cloudflated service stop

# 查看服务状态
Get-Service RokBJ*
```

---

## 🆚 与云服务器方案对比

| 对比项 | 本地电脑 + Tunnel | 云服务器 |
|--------|------------------|---------|
| 费用 | 免费 | 需要买服务器 |
| 性能 | 取决于你的电脑 | 取决于服务器配置 |
| 稳定性 | 电脑关机就断 | 7x24 运行 |
| 带宽 | 家庭宽带上传有限 | 独享带宽 |
| 适合场景 | 个人使用/开发测试 | 正式上线运营 |

---

## 🎯 推荐策略

- **开发测试阶段**：用本地电脑 + Cloudflare Tunnel（免费）
- **正式上线运营**：用云服务器（稳定可靠）

祝部署顺利！🎉
