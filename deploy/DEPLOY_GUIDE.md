# 🚀 搭伴 (rokbj.me) 部署指南

> **小白版**：从零开始把项目部署到阿里云服务器上

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
│   Nginx (80端口)                │
│                                 │
│   /           → 前端静态文件    │
│   /api/**     → 后端 (8080)     │
│   /ws/**      → 后端WebSocket   │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│   Spring Boot 后端 (8080)       │
│   ├── MySQL 数据库 (3306)       │
│   └── Redis 缓存 (6379)        │
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

> **解释**：`@` 表示主域名 `rokbj.me`，`www` 表示 `www.rokbj.me`
> 这两条记录都指向你的服务器公网 IP

### 1.3 等待解析生效

- 一般 **5-30 分钟**生效
- 可以在本地 cmd 执行 `ping rokbj.me` 检查
- 返回 `47.114.77.7` 就说明解析成功了 ✅

---

## 第二步：阿里云安全组开放端口

> ⚠️ 这一步**非常重要**！不开放端口，外网访问不了你的服务器！

### 2.1 进入安全组设置

1. 阿里云控制台 → **云服务器 ECS** → **实例**
2. 找到你的服务器 → 点击进去
3. 左侧菜单 → **安全组** → 点击当前安全组

### 2.2 添加安全规则

点击 **配置规则** → **入方向** → **手动添加**：

| 协议类型 | 端口范围 | 授权对象 | 说明 |
|---------|---------|---------|------|
| 自定义 TCP | 22/22 | 0.0.0.0/0 | SSH 远程登录 |
| 自定义 TCP | 80/80 | 0.0.0.0/0 | HTTP 网页访问 |
| 自定义 TCP | 443/443 | 0.0.0.0/0 | HTTPS（以后用） |

> ⚠️ **不要开放 3306 (MySQL) 和 6379 (Redis)**！
> 数据库和 Redis 只允许本机访问就够了，开放公网会有安全风险！

---

## 第三步：连接服务器并安装环境

### 3.1 远程连接服务器

**Windows 用户**：
- 下载 Windows Terminal（Win10/11 自带）或 PuTTY
- 打开终端，输入：
```bash
ssh root@47.114.77.7
```
- 输入 root 密码（在阿里云实例页面可以重置）

### 3.2 创建普通用户（安全起见）

```bash
# 创建用户
useradd -m -s /bin/bash rokbj

# 设置密码
passwd rokbj

# 添加 sudo 权限
usermod -aG sudo rokbj

# 创建项目目录
mkdir -p /home/rokbj/backend
mkdir -p /home/rokbj/frontend
mkdir -p /home/rokbj/uploads
mkdir -p /home/rokbj/logs

# 修改目录所有者
chown -R rokbj:rokbj /home/rokbj
```

### 3.3 更新系统

```bash
sudo apt update && sudo apt upgrade -y
```

### 3.4 安装 JDK 17

```bash
sudo apt install openjdk-17-jdk -y

# 验证安装
java -version
# 应该输出：openjdk version "17.x.x"
```

### 3.5 安装 Redis

```bash
sudo apt install redis-server -y

# 配置 Redis 密码（安全起见）
sudo nano /etc/redis/redis.conf
# 找到 # requirepass foobared 这一行
# 改成：requirepass 你的Redis密码

# 重启 Redis
sudo systemctl restart redis-server

# 设置开机自启
sudo systemctl enable redis-server

# 验证
redis-cli -a 你的Redis密码 ping
# 返回 PONG 就对了 ✅
```

### 3.6 安装 MySQL

```bash
# 安装 MySQL
sudo apt install mysql-server -y

# 启动 MySQL
sudo systemctl start mysql

# 设置开机自启
sudo systemctl enable mysql

# 设置 root 密码并创建数据库
sudo mysql -u root
```

在 MySQL 命令行里执行：
```sql
-- 如果 root 没有密码，先设置一个
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的MySQL密码';

-- 创建项目数据库
CREATE DATABASE make_friends DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 刷新权限
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

### 3.7 导入数据库表结构

把本地的 `make-friends-backend/sql/` 目录下的 SQL 文件上传到服务器：

```bash
# 在本地 Windows 执行（上传 SQL 文件到服务器）
scp e:\TraePorject\make-friends\make-friends-backend\sql\*.sql root@47.114.77.7:/tmp/
```

然后在服务器上执行：
```bash
# 登录 MySQL
mysql -u root -p

# 导入建表脚本
USE make_friends;
SOURCE /tmp/makefriends_schema.sql;
SOURCE /tmp/004_db_indexes.sql;
EXIT;
```

### 3.8 安装 Nginx

```bash
sudo apt install nginx -y

# 启动 Nginx
sudo systemctl start nginx

# 设置开机自启
sudo systemctl enable nginx

# 验证
curl http://localhost
# 应该返回 Welcome to nginx ✅
```

---

## 第四步：打包项目

### 4.1 打包后端

在本地 Windows 执行：
```bash
cd e:\TraePorject\make-friends\make-friends-backend
mvn clean package -DskipTests
```

打包完成后，在 `target/` 目录下会有：
```
make-friends-backend-1.0.0.jar
```

### 4.2 打包前端

在本地 Windows 执行：
```bash
cd e:\TraePorject\make-friends\make-friends-frontend
npm install
npm run build
```

打包完成后，会有 `dist/` 目录。

### 4.3 上传到服务器

```bash
# 上传后端 jar
scp e:\TraePorject\make-friends\make-friends-backend\target\make-friends-backend-1.0.0.jar rokbj@47.114.77.7:/home/rokbj/backend/

# 上传前端 dist（整个目录）
scp -r e:\TraePorject\make-friends\make-friends-frontend\dist rokbj@47.114.77.7:/home/rokbj/frontend/
```

---

## 第五步：部署后端

### 5.1 上传生产配置文件

```bash
# 在本地 Windows 执行
scp e:\TraePorject\6a7addb1ac58c8906b38a5cb\deploy\application-prod.yml rokbj@47.114.77.7:/home/rokbj/backend/
```

### 5.2 修改生产配置

登录服务器：
```bash
ssh rokbj@47.114.77.7
```

编辑配置文件：
```bash
nano /home/rokbj/backend/application-prod.yml
```

**必须修改的配置**：
1. `spring.datasource.password` → 改成你的 MySQL 密码
2. `spring.data.redis.password` → 改成你的 Redis 密码
3. `file.upload.path` → 确认是 `/home/rokbj/uploads/`

### 5.3 手动启动测试

```bash
cd /home/rokbj/backend

# 启动后端
java -Xms256m -Xmx512m \
    -jar make-friends-backend-1.0.0.jar \
    --spring.profiles.active=prod
```

看到 `Started MakeFriendsApplication` 日志就说明启动成功了！

**测试接口**（在服务器上执行）：
```bash
curl http://localhost:8080/api/actuator/health
# 应该返回 {"status":"UP"} ✅
```

**按 Ctrl+C 停止**，继续下一步。

### 5.4 配置 systemd 服务（开机自启）

在本地 Windows 执行：
```bash
scp e:\TraePorject\6a7addb1ac58c8906b38a5cb\deploy\rokbj-backend.service root@47.114.77.7:/etc/systemd/system/
```

在服务器上执行：
```bash
# 重载 systemd 配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start rokbj-backend

# 查看状态（应该显示 active (running)）
sudo systemctl status rokbj-backend

# 设置开机自启
sudo systemctl enable rokbj-backend

# 查看日志
sudo journalctl -u rokbj-backend -f
```

---

## 第六步：配置 Nginx

### 6.1 上传 Nginx 配置

在本地 Windows 执行：
```bash
scp e:\TraePorject\6a7addb1ac58c8906b38a5cb\deploy\nginx_rokbj.conf root@47.114.77.7:/etc/nginx/conf.d/rokbj.conf
```

### 6.2 修改前端文件路径

在服务器上执行：
```bash
sudo nano /etc/nginx/conf.d/rokbj.conf
```

确保这一行的路径正确：
```nginx
root /home/rokbj/frontend/dist;
```

### 6.3 测试并重载 Nginx

```bash
# 测试配置是否正确
sudo nginx -t
# 显示 syntax is ok, test is successful 就对了

# 重载配置
sudo systemctl reload nginx
```

---

## 第七步：验证部署

### 7.1 测试访问

在**本地电脑**浏览器访问：

1. **前端页面**：http://rokbj.me
   - 能看到登录页面 ✅

2. **API 接口**：http://rokbj.me/api/actuator/health
   - 返回 `{"status":"UP"}` ✅

3. **文件上传测试**：
   - 登录后尝试上传头像 ✅

### 7.2 检查 WebSocket（聊天功能）

打开聊天页面，发送一条消息测试 ✅

### 7.3 查看日志

```bash
# 后端日志
sudo journalctl -u rokbj-backend -f

# Nginx 访问日志
sudo tail -f /var/log/nginx/access.log

# Nginx 错误日志
sudo tail -f /var/log/nginx/error.log
```

---

## 🎉 恭喜！部署完成！

现在任何人都可以通过 `http://rokbj.me` 访问你的网站了！

---

## 📝 常用运维命令

```bash
# 查看后端状态
sudo systemctl status rokbj-backend

# 重启后端（更新代码后）
sudo systemctl restart rokbj-backend

# 查看实时日志
sudo journalctl -u rokbj-backend -f

# 重启 Nginx
sudo systemctl restart nginx

# 重载 Nginx 配置（修改配置后）
sudo systemctl reload nginx

# 查看服务器资源使用
htop  # 如果没装：sudo apt install htop -y
```

---

## 🔄 以后更新代码

1. 在本地重新打包：
```bash
# 后端
mvn clean package -DskipTests

# 前端
npm run build
```

2. 上传到服务器：
```bash
scp make-friends-backend-1.0.0.jar rokbj@47.114.77.7:/home/rokbj/backend/
scp -r dist/* rokbj@47.114.77.7:/home/rokbj/frontend/dist/
```

3. 重启服务：
```bash
sudo systemctl restart rokbj-backend
sudo systemctl reload nginx
```

---

## ⚠️ 常见问题

### Q: 访问域名显示 "502 Bad Gateway"
**原因**：后端没有运行
**解决**：`sudo systemctl status rokbj-backend` 查看后端状态

### Q: 页面空白或 404
**原因**：前端文件路径不对
**解决**：检查 Nginx 配置中的 `root` 路径

### Q: API 请求失败
**原因**：后端没启动或 Nginx 代理配置错误
**解决**：检查 Nginx 配置中的 `proxy_pass` 是否指向 `127.0.0.1:8080`

### Q: 上传文件失败
**原因**：上传目录权限问题
**解决**：`sudo chmod 755 /home/rokbj/uploads/`

### Q: WebSocket 连接失败
**原因**：Nginx 缺少 WebSocket 配置
**解决**：检查 Nginx 配置中的 `/ws/` location 块

---

## 🔒 安全建议（可选，重要）

1. **配置 SSL 证书**（HTTPS）：
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d rokbj.me -d www.rokbj.me
```
证书自动续期，配置后访问 https://rokbj.me

2. **配置防火墙规则**：
```bash
sudo ufw enable
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
```

3. **定期备份数据库**：
```bash
# 手动备份
mysqldump -u root -p make_friends > backup_$(date +%Y%m%d).sql

# 恢复备份
mysql -u root -p make_friends < backup_20260817.sql
```

---

祝你部署顺利！🎉🎉🎉
