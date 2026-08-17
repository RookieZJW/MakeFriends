# 搭伴 Backend · 校园互动平台后端 ☕

基于 **Spring Boot 3.3 + Java 17 + MyBatis-Plus + Sa-Token + Redis + Resilience4j + WebSocket** 构建的校园互动平台后端服务。为前端（Vue 3）提供 RESTful API + 实时聊天 WebSocket 双通道。

- 基础路径：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- 健康检查：`http://localhost:8080/api/actuator/health`

---

## 🧱 技术选型

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.3.x | 主框架 / 自动装配 / Actuator 监控 |
| Java | 17 | LTS，record / pattern matching |
| MyBatis-Plus | 3.5.7 | BaseMapper CRUD + PaginationInnerInterceptor |
| Sa-Token | 1.38.0 | `StpUtil.login()` 登录、`@SaCheckLogin` 鉴权；**Redis 持久化** (`sa-token.type=redis`) |
| Spring Data Redis | - | Redis 客户端：`RedisCacheService` 热点缓存 + 限流计数器 |
| Resilience4j | 2.2.0 | 熔断（失败率≥50%，20s 半开） + 重试（最多 2 次，含首次=1次重试） |
| Spring AOP | - | 支撑 `@CircuitBreaker` / `@RateLimiter` / `@Retry` 注解切面 |
| Spring WebSocket | - | 独立注册 `/ws/chat` 端点，实时消息推送（`ConcurrentHashMap` 线程安全存储） |
| SpringDoc OpenAPI | 2.6.0 | Swagger UI + OpenAPI 3.0 自动文档 |
| HikariCP | - | Spring Boot 自带，`maximum-pool-size=50` 高并发登录保护 |
| MySQL Connector / J | 8.0 | 驱动 MySQL 8，配合 5 张表的复合覆盖索引 |
| Lombok | - | `@Data / @Builder / @Slf4j` |
| spring-boot-starter-validation | - | `@NotBlank / @Size` 参数校验 + `@Valid` 绑定 `BindingResult` |

---

## 🛡 三层防护架构

```
请求进入
   │
   ▼
③ GlobalRateLimitFilter (HandlerInterceptor, order=0 最高优先级)
     ├─ IP白名单 + 路径白名单 + X-Forwarded-For 真实 IP 解析
     ├─ 单 IP 每分钟上限 180 次（RedisCacheService.incrEx 分桶计数）
     ├─ 超限抛 TooManyRequestsException(429, retryAfter=60s)
     └─ 登录失败：5次错误/60秒窗口 → 账号锁定 10 分钟（RateLimitService）
   │
   ▼
② 数据库层保护
     ├─ HikariCP: maximum-pool-size=50, minimum-idle=10
     ├─ MySQL 复合覆盖索引:
     │   ├─ idx_session_created(session_id, created_at DESC)
     │   ├─ idx_session_receiver_read(3 列)
     │   ├─ idx_user_created(user_id, created_at DESC)
     │   ├─ idx_status_created(status, created_at DESC)
     │   └─ idx_dynamic_created
     ├─ 关键写操作 @Transactional(rollbackFor=Exception.class) + @CircuitBreaker(name="xxx") + @Retry(name="mysql-retry")
     └─ 熔断参数: sliding-window-size=20, min-calls=8, failure-rate=50%, wait-in-open=20s
   │
   ▼
① 缓存层 Cache-Aside Pattern (RedisCacheService)
     ├─ Sa-Token token 存储: type=redis, 支持分布式部署 & 减轻 DB 查询
     ├─ 热点用户详情 / 字典数据缓存
     └─ 限流计数器: rate-limit:ip:{ip} / login-lock:phone:{phone} / login-fail:{phone}:{bucket}
```

> `MyBatis-Plus log-impl` 生产配置 `org.apache.ibatis.logging.nologging.NoLoggingImpl` 关闭控制台 SQL，避免性能损耗。

---

## 📦 依赖管理（pom.xml 关键片段）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.2</version>
</parent>

<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <sa-token.version>1.38.0</sa-token.version>
    <resilience4j.version>2.2.0</resilience4j.version>
</properties>

<dependencies>
    <!-- 基础 starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>

    <!-- Resilience4j: 熔断 + 限流 + 重试 -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
        <version>${resilience4j.version}</version>
    </dependency>

    <!-- Sa-Token + Redis 持久化 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>${sa-token.version}</version>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!-- MyBatis-Plus / MySQL / 驱动 -->
    ...
</dependencies>
```

---

## 🚀 启动方式

### 方式 A：Maven 命令行（推荐）

```powershell
$env:JAVA_HOME = "D:\JDK"
$env:MAVEN_HOME = "D:\apache-maven-3.9.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

cd make-friends-backend
mvn spring-boot:run
```

### 方式 B：IDE（IntelliJ IDEA / VSCode）

直接运行 `MakeFriendsApplication.main()`。需要：
- Project SDK = JDK 17
- Maven 配置指向本机 3.9+

### 方式 C：打包成 jar（生产使用）

```powershell
mvn clean package -DskipTests
# → target/make-friends-backend-1.0.0.jar

java -Xms256m -Xmx512m -jar target\make-friends-backend-1.0.0.jar `
  --spring.datasource.password='线上密码' `
  --spring.profiles.active=prod
```

生产环境示例配置：`../deploy/application-prod.yml`

---

## ⚙️ application.yml 配置详解

路径：`src/main/resources/application.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /api              # 所有接口统一前缀 /api

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/makefriends?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8
    username: root
    password: 请改成你本机的 MySQL 密码
    hikari:
      maximum-pool-size: 50         # 🔴 高并发登录保护
      minimum-idle: 10
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # password: （可选）
      lettuce:
        pool:
          max-active: 20
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB

# Sa-Token 持久化到 Redis
sa-token:
  token-name: satoken
  timeout: 2592000                   # 30 天
  is-concurrent: true
  token-style: uuid
  type: redis                        # 🔴 关键：改 Redis 存储，不再走内存
  is-share: true

# Resilience4j（熔断 + 重试）
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 20
        minimum-number-of-calls: 8
        failure-rate-threshold: 50
        slow-call-duration-threshold: 3s
        wait-duration-in-open-state: 20s
    instances:
      mysql-default:
        base-config: default
  ratelimiter:
    configs:
      default:
        limit-for-period: 50
        limit-refresh-period: 1s
  retry:
    configs:
      default:
        max-attempts: 2             # 含首次 = 重试 1 次
        wait-duration: 100ms

# 限流/登录锁参数
mf:
  rate-limit:
    enabled: true
    ip-limit-per-min: 180
    login-fail-threshold: 5
    login-fail-window-seconds: 60
    login-lock-seconds: 600

# 文件上传路径
file:
  upload:
    path: E:/TraePorject/make-friends/uploads/   # 必须是绝对路径！

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl  # 🔴 生产关 SQL 日志
  global-config:
    db-config:
      id-type: auto
```

> ⚠️ 生产环境请把 `sa-token.timeout` 调小（如 7 天），把 Redis / MySQL 密码改为强密码，Sa-Token 拦截器务必排除 `/upload/**`、`/actuator/**`、`/api/files/**` 三个路径（见 `WebConfig` 与 `SaTokenConfig`）。

---

## 🗄 数据库初始化

表结构文件放在 `sql/`：

```
sql/
├── makefriends_schema.sql          # 9 张表 DDL（SHOW CREATE TABLE 真实导出，含索引/外键）
└── 004_db_indexes.sql              # 5 张热点表复合覆盖索引（新建或修复）
```

一键导入：

```powershell
mysql -u root -p makefriends < sql/makefriends_schema.sql
mysql -u root -p makefriends < sql/004_db_indexes.sql
```

### 9 张实体表 ↔ Entity ↔ Mapper 对照

| 表名 | Entity | Mapper |
|---|---|---|
| users | User | UserMapper |
| user_matches | UserMatch | UserMatchMapper |
| chat_sessions | ChatSession | ChatSessionMapper |
| chat_messages | ChatMessage | ChatMessageMapper |
| user_dynamics | UserDynamic | UserDynamicMapper |
| dynamic_comments | DynamicComment | DynamicCommentMapper |
| dynamic_likes | DynamicLike | DynamicLikeMapper |
| hobby_dict | HobbyDict | HobbyDictMapper |
| occupation_dict | OccupationDict | OccupationDictMapper |

所有 Mapper 均继承 `BaseMapper<T>`；复杂分页查询在对应 `ServiceImpl` 写 `LambdaQueryWrapper`。

---

## 🧭 代码结构速查

```
com.makefriends
├── MakeFriendsApplication.java              # @SpringBootApplication + @MapperScan
├── common
│   ├── Result.java / ResultCode.java        # 统一 Result<T> {code, msg, data}
│   ├── GlobalExceptionHandler.java          # @RestControllerAdvice，含 429/503/401 分类
│   ├── RedisCacheService.java               # Cache-Aside 封装：set/get/del/incrEx
│   ├── RateLimitService.java                # IP 限流 + 登录失败计数 + 锁定
│   ├── TooManyRequestsException.java        # 自定义异常，带 retryAfterSeconds
│   └── PasswordUtil.java
├── config
│   ├── MyBatisPlusConfig.java               # PaginationInnerInterceptor
│   ├── SaTokenConfig.java                   # Sa-Token 拦截 + 3 类路径白名单排除
│   ├── WebConfig.java                       # CORS + 静态资源 + **GlobalRateLimitFilter 注册(order=0)**
│   ├── GlobalRateLimitFilter.java           # IP限流拦截器：X-Forwarded-For取真实IP
│   └── WebSocketConfig.java                 # /ws/chat + HandshakeInterceptor 取 satoken
├── controller (共 10 个)
│   ├── AuthController                       # register/login/logout；先检查锁定→失败计数→成功清除
│   ├── UserController                       # me/detail/list/edit；敏感字段（phone/password/status）拦截
│   ├── UserMatchController                  # like/unlike/status/my-likes/mutual + tacit三维默契度算法
│   ├── DynamicController                    # publish 含 @CircuitBreaker @Retry
│   ├── CommentController                    # 二级评论：findRootId()追溯根，统一挂根节点
│   ├── LikeController
│   ├── ChatController                       # sendMessage 单@Transactional + @CircuitBreaker；DELETE会话双删联动
│   ├── UploadController                     # 上传路径绝对路径；返回URL前缀/api/files/
│   ├── HobbyDictController / OccupationDictController
├── service + impl
│   ├── UserServiceImpl.login                # RateLimitService 锁定先判→失败计数→成功清锁
│   ├── DynamicServiceImpl.publish           # @CircuitBreaker(name="user-dynamic-db") + @Retry
│   ├── ChatServiceImpl.sendMessage          # @Transactional + @CircuitBreaker(name="chat-channel-db") + @Retry
│   └── ...
├── mapper / entity / dto / vo
├── websocket
│   └── ChatWebSocketHandler.java            # TextWebSocketHandler + ConcurrentHashMap<userId,Session>
└── util
```

---

## 🔐 鉴权 + 限流规则

### Sa-Token

- **Header Key**：`satoken`（不是 Authorization Bearer）
- **强制登录重置**：每次登录 `onlineStatus` 强制重置为 1（在线）
- **在线判定**：`onlineStatus=1` 且 `lastActiveAt` 距当前 ≤ 60 秒 → 在线；`onlineStatus=2`(隐身) 或超时 → 离线
- **白名单排除**（见 WebConfig + SaTokenConfig）：
  - `/upload/**`, `/actuator/**`, `/api/files/**`（Sa-Token 拦截排除）
  - `/auth/register`, `/auth/login`, `GET /actuator/health`, `swagger**`, `v3/api-docs**`

### IP 限流 + 登录锁定（GlobalRateLimitFilter + RateLimitService）

```java
// 每次请求经过 Filter：
String realIp = resolveRealIp(req);  // 解析 X-Forwarded-For
if (rateLimitService.isIpOverLimit(realIp))
    throw new TooManyRequestsException("请求过于频繁，请稍后再试", 60L);

// 登录：
if (isLoginLocked(phone))  throw new TooManyRequestsException("密码错误次数过多...", remain);
onLoginFailed(phone);      // 失败计数
onLoginSuccess(phone);     // 成功清除锁定 + 失败计数
```

### GlobalExceptionHandler HTTP 状态码

| 异常 | HTTP 状态 | 返回 |
|------|---------|------|
| `TooManyRequestsException` | 429 Too Many Requests | `Result.fail(429, msg)` + `Retry-After` header |
| `CallNotPermittedException`（熔断打开） | 503 Service Unavailable | `Result.fail(503, "服务暂不可用，请稍后再试")` |
| `RequestNotPermitted`（Resilience4j RateLimiter） | 429 | 同限流 |
| `NotLoginException`（Sa-Token） | 401 | `Result.fail(401, "未登录或登录过期")` |

---

## 📡 聊天双通道

| 通道 | URL | 场景 |
|---|---|---|
| HTTP（主） | `POST /api/chat/send` | 发送消息；前端 2s 一次轮询拉取历史 |
| WebSocket（辅） | `ws://host:8080/api/ws/chat?satoken=xxx` | 双方在线时秒级推送；掉线自动降级为 HTTP 轮询 |

- **会话 ID 生成**：首次聊天自动按 `min(senderId,receiverId) / max(...)` 规则生成，避免双向会话不一致
- **双删联动**：DELETE `/chat/session/{sid}` 置对应 `user1_deleted` 或 `user2_deleted=1`；两者都为 1 → 物理删 `chat_sessions` + `chat_messages`
- **发消息到已删会话**：自动把发送方的 `deleted` 标记重置为 0
- **消息分页**：`pageSize=20`，新消息倒序；前端懒加载首屏 20 条

WebSocket 上行 JSON：
```json
{ "type": "chat", "toSessionId": 12, "content": "今晚一起吃饭？", "msgType": 1 }
```

---

## 🧪 自测清单（上线前必过）

1. ✅ `GET /api/actuator/health` → `{"status":"UP"}`
2. ✅ `POST /api/auth/login` 连续 5 次输错密码 → 第 6 次返回 429（锁定）
3. ✅ 单 IP 一分钟内连续请求 >180 → 之后返回 429
4. ✅ `POST /api/auth/login` 正确密码 → 返回 200 + satoken；online_status 重置为 1
5. ✅ `GET /api/user/list` 带 Header `satoken` → 返回分页；`PUT /user/me` 无法改 phone/password/status
6. ✅ `POST /api/dynamic` 发一条动态 → 自动走熔断/重试切面
7. ✅ `POST /api/match/like/2` 双向喜欢后自动建 chat_sessions（min/max id）
8. ✅ `DELETE /api/chat/session/{sid}` 再发消息 → 删除标记自动重置为 0
9. ✅ `GET /api/chat/messages/{sid}?page=1&pageSize=20` → `records + total` 字段
10. ✅ Redis keys 检查：`satoken:login:session:*`、`rate-limit:ip:*`、`login-lock:*` 是否存在

---

## 🛠 常见问题排查

- **启动报 "Communications link failure"** → 检查 `application.yml` MySQL 密码 + 3306 监听；Windows 防火墙放行 MySQL
- **启动报 "Redis 连接失败"** → `redis-cli ping` 是否 PONG；Windows 版 Redis 默认无 `daemonize`，开一个 cmd 窗口跑 `redis-server.exe`
- **Sa-Token 401 登不上** → 确认请求 Header 键是小写 `satoken`（不是 `Authorization` / `Bearer`），浏览器 DevTools → Application → Local Storage 查看 satoken
- **启动报 "非法字符 \ufeff"** → RedisCacheService.java 等 Java 源文件是不是 UTF-8 with BOM，改为 UTF-8 无 BOM 重新保存
- **RateLimit 报 Redis 连接异常** → `spring.data.redis.password` 留空时不要写 `password:` 这一行，或用 ~ 空值占位
- **Resilience4j 切面不生效** → pom 是否加了 `spring-boot-starter-aop`；注解必须写在 Spring 托管 Bean（Service）上，且由外部方法调用（同类自调用无效）
- **文件上传 404 刷新看不到** → `upload.path` 必须用**绝对路径** `${user.dir}/uploads/` 之类，WebConfig 把 `/api/files/**` 映射到该目录，并确保 Sa-Token 拦截器排除它
- **8080 端口占用** → `netstat -ano | findstr :8080` → `taskkill /PID <id> /F`

更多部署相关的 FAQ，查看 `deploy/DEPLOY_LOCAL_WINDOWS.md` 的「常见问题排查」章节。
