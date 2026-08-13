# MakeFriends Backend · 交友平台后端 ☕

基于 **Spring Boot 3.3 + Java 17 + MyBatis-Plus + Sa-Token + WebSocket** 构建的交友网站后端服务。为前端（Vue 3）提供 RESTful API + 实时聊天 WebSocket 双通道。

- 基础路径：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- 健康检查：`http://localhost:8080/api/actuator/health`

---

## 🧱 技术选型

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.3.x | 主框架 / 自动装配 / Actuator 监控 |
| Java | 17 | LTS，sealed class / record / pattern matching |
| MyBatis-Plus | 3.5.7 | BaseMapper CRUD + PaginationInnerInterceptor |
| Sa-Token | 1.38.0 | `StpUtil.login()` 登录、`@SaCheckLogin` 鉴权、satoken Header 传递 |
| Spring WebSocket | - | 独立注册 `/ws/chat` 端点，实现实时消息推送 |
| SpringDoc OpenAPI | 2.6.0 | Swagger UI + OpenAPI 3.0 自动文档 |
| MySQL Connector / J | 8.0 | 驱动 MySQL 8 |
| Lombok | - | `@Data / @Builder / @Slf4j` |
| Apache Commons Lang3 | - | `StringUtils`, `CollectionUtils` 等 |
| spring-boot-starter-validation | - | `@NotBlank / @Size` 参数校验 |

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
</properties>
```

已经集成的 starters：`web`, `validation`, `websocket`, `actuator`。

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
- Maven 配置指向自带或本机安装（推荐自带 3.9+）

### 方式 C：打包成 jar（生产使用）

```powershell
mvn clean package -DskipTests
# target/make-friends-backend-0.0.1-SNAPSHOT.jar

java -jar target/make-friends-backend-0.0.1-SNAPSHOT.jar `
  --spring.datasource.password='线上密码'
```

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
    url: jdbc:mysql://localhost:3306/make_friends?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8
    username: root
    password: 请改成你本机的 MySQL 密码
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true    # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto                  # 主键自增
      logic-delete-field: deleted    # 预留逻辑删除字段

sa-token:
  token-name: satoken
  timeout: 2592000                   # 30 天
  is-concurrent: true                # 同账号多端登录
  token-style: uuid                  # token 风格

upload:
  path: E:/TraePorject/make-friends/make-friends-backend/uploads
```

> ⚠️ 生产环境请把 `sa-token.timeout` 调小（如 7 天），并把 `upload.path` 改成服务器上的绝对路径。

---

## 🗄 数据库初始化

9 张表 + 字典数据文件放在 `sql/`：

```
sql/
├── makefriends_schema.sql           # 9 张表 DDL（SHOW CREATE TABLE 导出，含索引/外键）
└── 003_hobby_occupation_dict.sql    # hobby_dict 180+ / occupation_dict 200+
```

一键导入：

```powershell
mysql -u root -p make_friends < sql/makefriends_schema.sql
mysql -u root -p make_friends < sql/003_hobby_occupation_dict.sql
```

### 9 张实体表 ↔ MyBatis-Plus Entity ↔ Mapper 对照

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

所有 Mapper 均继承 `BaseMapper<T>`；复杂分页查询在对应的 ServiceImpl 里写 `LambdaQueryWrapper`。

---

## 🧭 代码结构速查

```
com.makefriends
├── MakeFriendsApplication.java              # @SpringBootApplication + @MapperScan
├── common
│   ├── Result.java                          # 统一返回 Result<T> {code, msg, data}
│   ├── ResultCode.java                      # 200 OK / 401 UNAUTHORIZED / 500 ERROR 等
│   └── GlobalExceptionHandler.java          # @RestControllerAdvice + @ExceptionHandler
├── config
│   ├── MyBatisPlusConfig.java               # PaginationInnerInterceptor
│   ├── SaTokenConfig.java                   # Sa-Token 路由拦截 + 放行白名单
│   ├── WebConfig.java                       # CORS + 静态资源映射 upload.path → /uploads/**
│   └── WebSocketConfig.java                 # 注册 /ws/chat + HandshakeInterceptor 取 satoken
├── controller (共 10 个)
│   ├── AuthController                       # register / login / logout
│   ├── UserController                       # me / user/{id} / list / edit
│   ├── UserMatchController                  # like / unlike / status / my-likes / mutual
│   ├── DynamicController                    # list / detail / user / my / publish / delete
│   ├── CommentController                    # add / delete / list-by-dynamic
│   ├── LikeController                       # toggle / check
│   ├── ChatController                       # sessions / messages?page / send / read / unread
│   ├── UploadController                     # MultipartFile → /uploads/2025/xx.jpg URL
│   ├── HobbyDictController                  # 爱好字典（按分类分组）
│   └── OccupationDictController             # 职业字典
├── service
│   ├── impl/                                # 7 个 ServiceImpl extends ServiceImpl
│   ├── UserService / UserMatchService / DynamicService
│   ├── CommentService / LikeService / ChatService
│   └── AuthService / FileUploadService
├── mapper                                   # 9 个 Mapper extends BaseMapper<T>
├── entity                                   # 9 个 @TableName 实体
├── dto                                      # LoginDTO / RegisterDTO / SendMessageDTO 等
├── vo                                       # UserVO / SessionVO / MessageVO / DynamicVO 等
├── websocket
│   ├── ChatWebSocketHandler.java            # TextWebSocketHandler: afterConnectionEstablished / handleTextMessage / afterConnectionClosed
│   └── ChatEndpoint.java                    # 维护 WebSocket 会话 Map<userId, Session>
└── util
    ├── FileUploadUtil.java                  # 按日期分目录 + UUID 命名 + 扩展名保留
    └── PasswordUtil.java                    # BCrypt + 盐值校验（如使用）
```

---

## 🔐 鉴权规则（Sa-Token）

- **Header**：`satoken: <登录返回的 token>`
- **白名单（无需登录）**：
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/actuator/health`
  - `GET /api/swagger-ui/**` + `GET /api/v3/api-docs/**`
  - `GET /api/uploads/**`（图片静态资源）
- 其他所有接口都需要 `@SaCheckLogin` 或全局拦截。

---

## 📡 聊天双通道

| 通道 | URL | 场景 |
|---|---|---|
| HTTP（主） | `POST /api/chat/send` | 发送消息；前端 2s 一次轮询拉取历史 |
| WebSocket（辅） | `ws://host:8080/api/ws/chat?satoken=xxx` | 双方在线时秒级推送；掉线自动降级为 HTTP 轮询 |

WebSocket 上行消息 JSON 格式：

```json
{
  "type": "chat",
  "toSessionId": 12,
  "content": "今晚一起吃饭？",
  "msgType": 1
}
```

---

## 🧪 自测清单（上线前必过）

1. ✅ `GET /api/actuator/health` → `{"status":"UP"}`
2. ✅ `POST /api/auth/login` 用 `13800000001 / Test123456` → 返回 200 + satoken
3. ✅ `GET /api/user/list` 带 Header `satoken` → 返回分页列表
4. ✅ `POST /api/dynamic` 发一条带图动态 → `/api/dynamic/list` 能查到并 count+1
5. ✅ `POST /api/match/like/2` → `GET /api/match/mutual` 验证双向喜欢后自动建 chat_sessions
6. ✅ `GET /api/chat/messages/{sessionId}?page=1&pageSize=20` → 返回 records 数组 + total 字段

---

## 🛠 常见问题排查

- **启动报数据库错误** → 打开 `application.yml` 把 `mybatis-plus.configuration.log-impl=StdOutImpl` 打开，查看 SQL 输出。
- **Sa-Token 401** → 确认请求头的 key 是小写 `satoken`（不是 `Authorization` / `Bearer`）。
- **上传文件 413 Payload Too Large** → 调大 `spring.servlet.multipart.max-file-size` 和 Nginx 的 `client_max_body_size`。
- **WebSocket 连不上** → Nginx 必须加 `Upgrade` / `Connection` 头，见根目录 README「生产部署」。
