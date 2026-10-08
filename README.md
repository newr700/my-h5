# my-h5-server

`my-h5-app`（Vue3 前端）的 Java 后端。技术栈：Java 17 + Spring Boot 3.5 + Maven。

> 需求与技术裁定见前端仓库 `my-h5-app/docs/`：
> 《PRD-项目需求文档》《工程实施手册》《后端架构与上线全流程路线》。**先读文档再动手。**

## 技术栈组成

### 运行环境与框架

| 分类 | 技术 | 版本 | 在本项目里的职责 |
|---|---|---|---|
| 语言 | Java | 17（本机 Temurin 17.0.4.1） | 编译运行环境，最低要求即 17 |
| 构建 | Maven | 3.9.12 | 依赖管理与打包（本机 AI 内嵌终端跑不了，用系统 PowerShell/Windows Terminal） |
| 框架 | Spring Boot | 3.5.16 | 应用骨架、自动配置、内嵌 Tomcat；**3.x 已 EOL，选它是因为 MP/knife4j 对 4.x 兼容不稳，理由见选型文档 2.1，并写进 ADR-004** |
| Web | spring-boot-starter-web | —（随 Boot） | 同步阻塞 MVC 模型（一请求一线程），提供 REST 接口 |

### 数据层

| 技术 | 版本 | 职责 | 选型备注 |
|---|---|---|---|
| MyBatis-Plus | 3.5.16 | ORM，简单 CRUD 免写 SQL | 复杂查询仍手写 XML SQL，避免丢失 SQL 能力 |
| mybatis-plus-jsqlparser | 3.5.16 | 分页插件（3.5.9 起拆出独立模块） | 不引则分页静默失效，新手常见坑 |
| MySQL Connector/J | 随 Boot BOM | MySQL 驱动（运行时依赖） | 本机 MySQL 8，`my_h5` 库 utf8mb4 |
| Flyway Core | 随 Boot BOM | 数据库版本管理（迁移脚本） | 表结构变更必须写成 `Vx__xxx.sql` |
| Flyway MySQL | 随 Boot BOM | Flyway 10+ 的 MySQL 数据库支持模块 | 不引则启动报 `Unsupported Database: MySQL` |
| MySQL 8（数据库产品） | 8.0.44（本机） | 真实关系型数据库 | 生产可平滑替换为托管 PostgreSQL |

### 接口文档与开发效率

| 技术 | 版本 | 职责 | 选型备注 |
|---|---|---|---|
| knife4j（OpenAPI3 jakarta） | 4.5.0 | 接口文档 + 在线调试页 `/doc.html` | Boot 3 必须用 `-jakarta-` 坐标；**生产必须关闭 `/doc.html`，否则接口裸奔** |
| Lombok | 随 Boot | 编译期消除 getter/setter/构造器等样板 | 注解处理器改 AST，非反射 |

### 鉴权（W3）

| 技术 | 版本 | 职责 | 选型备注 |
|---|---|---|---|
| jjwt（api/impl/jackson 三件套） | 0.13.0 | JWT 签发与验签 | 0.12 起 API 大改，旧教程代码会编译不过 |
| Hutool（core + crypto） | 5.8.46 | BCrypt 密码哈希 | 按需引入，不用 `hutool-all`；BCrypt 在 `crypto.digest` 子包 |

### 横切 / 基础设施

| 技术 | 版本 | 职责 |
|---|---|---|
| spring-boot-starter-validation | 随 Boot | 后端参数校验（`@NotNull` 等），「前后端双校验」的后端一半 |
| spring-boot-starter-actuator | 随 Boot | 探活 `/actuator/health`（部署平台健康检查用） |
| spring-boot-maven-plugin | 随 Boot | 打出内嵌 Tomcat 的可执行 fat jar |

### 测试（test scope）

| 技术 | 版本 | 职责 |
|---|---|---|
| spring-boot-starter-test | 随 Boot | 单测 + 集成测试基础 |
| H2 Database | 随 Boot | 内存数据库，CI 不依赖本机 MySQL；用 MySQL 兼容模式跑同一份 Flyway 迁移，保证测的是真实表结构 |

> 依赖版本统一收敛在 `pom.xml` 的 `<properties>`（工程手册第 8 章：依赖引入原则）。每个依赖"为什么选、为什么是这个值"在 `pom.xml` 注释里有逐条说明，也与前端仓库 `docs/后端架构与上线全流程路线.md`、`ADR-004`、`ADR-005` 互相对应。

## 架构概览

按功能分包，分层红线清晰：

| 包 | 职责 |
|---|---|
| `auth` | JWT 签发验签、登录注册、认证拦截器 |
| `user` | 用户实体与 Mapper |
| `match` | 比赛（场次）查询与库存 |
| `order` | 下单 / 取消 / 支付 写闭环 |
| `standings` | 示例积分榜接口 |
| `common` | 统一响应 `Result`、业务异常 `BizException`、全局异常处理器 `GlobalExceptionHandler`、错误码 `ErrorCodes` |
| `config` | CORS、MyBatis-Plus、Jackson、WebMvc 等配置 |

**分层红线**：Controller → Service → Mapper，不允许反向或跨层直调；出入参用 DTO/VO 隔离，不把 Entity 直接出网。所有业务错误走 `BizException` + 错误码，由 `GlobalExceptionHandler` 统一包成 `{code,message,data}`。

**横切**：`AuthInterceptor` 校验 JWT 并写入 `AuthContext`（ThreadLocal）；`RequestContextInterceptor` 采集 IP/UA 写入 `RequestContext`。两个上下文**用后必须 clear**，否则线程复用会串数据。

## 数据库迁移（Flyway）

表结构变更全部写成迁移脚本，放在 `src/main/resources/db/migration/`，**只增不改、绝不允许改名或删除已应用的迁移**：

| 脚本 | 作用 |
|---|---|
| `V1__init.sql` | 建表（user / match_info / match_order / team_standing 等） |
| `V2__seed.sql` | 灌入 8 场比赛初始数据 |
| `V3__stock.sql` | 给 `match_info` 加 `total_stock` / `stock` 库存字段（V2 Step1 防超卖） |
| `V4__idempotency.sql` | 订单 `request_id` + 唯一索引（V2 Step2 幂等，已完成） |
| `V5__audit.sql` | 审计日志表 `audit_log`（V2 Step3 审计，已完成） |
| `V6__avatar.sql` | `app_user` 加 `avatar_url`；头像文件存磁盘、DB 存相对路径（头像上传） |

## 错误码

`common/ErrorCodes.java` 是错误码的唯一来源（前端 `request.ts` 对 1101/1102 有特殊处理）：

| 码 | 含义 |
|---|---|
| 1001 | 参数校验失败 |
| 1002 | 资源不存在 |
| 1101 | 未登录 / token 缺失或非法 |
| 1102 | token 已过期（前端跳登录） |
| 2001 | 注册：用户名已存在 |
| 2002 | 登录：用户名或密码错误（模糊，防枚举） |
| 3001 | 订单不存在（或不是你的，故意同码防枚举） |
| 3002 | 订单状态机守卫：当前状态不允许该操作 |
| 3003 | 库存不足（V2 Step1 新增） |
| 4001 | 文件上传：文件为空 |
| 4002 | 文件上传：类型不支持（仅允许 JPG / PNG / WebP） |
| 5000 | 系统内部错误（细节仅进日志） |

## 项目状态与实施进度（V2 正确性加固）

V2 是把「能跑通」升级为「真实场景不出错」的阶段，拆成三次独立交付，每步固定走「讲清楚 → 写代码 → 跑测试 → git 提交」：

| Step | 目标 | 状态 | 迁移脚本 | 提交 |
|---|---|---|---|---|
| Step 1 | 防超卖（并发抢票不超卖） | 已完成 | `V3__stock.sql` | `1e2ea8e` |
| Step 2 | 幂等（同一请求只处理一次，防重复下单） | 已完成 | `V4__idempotency.sql` | `9a3f641` |
| Step 3 | 审计日志（谁改了什么可追溯） | 已完成 | `V5__audit.sql` | `24206b7` |
| Step 4 | 头像上传（文件存磁盘、DB 存相对路径、静态资源映射 `/uploads`） | 已完成 | `V6__avatar.sql` | `ec1f74d` |

- **防超卖（已完成）**：`MatchInfoMapper.deductStock` 用「带条件的原子 UPDATE」（`WHERE stock >= qty`）靠行锁消除并发下的 TOCTOU；下单先扣库存，取消回补，支付不回补。`StockIntegrationTest` 6 项验证。
- **幂等（已完成）**：`CreateOrderRequest` 携带必填 `requestId`（@NotBlank），订单表加 `(user_id, request_id)` 唯一索引；下单前按 `(userId, requestId)` 预检，命中返还原订单且不重复扣库存，撞索引回滚并抛 3004 让前端重试。前端 `api/order.ts` 用 `pendingRequestId`（成功清空、失败保留）配合。测试见 `IdempotencyIntegrationTest`（5 项）。
- **审计（已完成）**：新增 `audit_log` 表（V5 迁移）。`OrderService` 的 `create`/`pay`/`cancel` 三个写操作，在**成功路径**与**失败路径**（状态机守卫 3002 / 订单不存在 3001 / 库存不足 3003 / 并发幂等 3004）都记一笔审计，含 actor、action、状态迁移、错误码、关联幂等号。关键设计：`AuditLogService.record()` 用 `@Transactional(REQUIRES_NEW)` 独立提交，确保主业务即便回滚，失败尝试的审计也不丢失；审计表因此**不建外键**指向订单表。测试见 `AuditLogIntegrationTest`（6 项）。
- **头像上传（已完成，`ec1f74d`）**：`V6__avatar.sql` 给 `app_user` 加 `avatar_url` 列。`UserService.uploadAvatar` 把图片存到本地磁盘（`app.upload-dir`，默认 `./uploads`，已 gitignore），DB 只存相对路径 `/uploads/xxx.png`；`WebMvcConfig` 把 `/uploads/**` 映射为可访问 URL，否则文件躺在磁盘浏览器取不到。前端在用户中心页选图，走自封装 axios（带 token、`src/api/user.ts`）上传，成功后刷新资料，右上角与用户中心同步显示头像。安全四件套：仅允许 JPG/PNG/WebP、上限 2MB、文件名随机防遍历、旧头像自动删除防磁盘膨胀。测试见 `AvatarUploadIntegrationTest`（6 项）。

## 测试

```bash
mvn clean test
```

- 在 H2（MySQL 兼容模式）下跑**同一份** Flyway 迁移，保证测的是真实表结构、不依赖本机 MySQL。
- 当前 **38/38 全绿**（ApiFlow 10 + JWT 4 + 冒烟 1 + 库存集成 6 + 幂等 5 + 审计 6 + 头像上传 6）。
- 用系统 PowerShell / Windows Terminal 跑（本机 AI 会话内置终端跑不了 mvn，已知环境问题）。
- 切分支 / 回退后务必 `mvn clean`，否则 `target/classes` 残留旧迁移脚本会导致 Flyway 报「找到多个相同版本的迁移」。

## 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/actuator/health` | 探活 |
| GET | `/doc.html` | knife4j 接口文档与调试页（生产须关闭） |
| GET | `/standings` | 示例积分榜 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| GET | `/api/match/matches` | 在售比赛列表（含 `totalStock` / `stock`） |
| POST | `/api/order` | 下单 |
| 其余 | `/api/order/**` | 订单查询 / 取消 / 支付（具体路径见 `/doc.html`） |
| POST | `/api/user/avatar` | 上传头像（multipart/form-data，字段名 `file`；仅 JPG/PNG/WebP，≤2MB） |

> 完整字段与示例见 `/doc.html`，以实际代码为准。

## 快速开始

**用 Windows 自带的 PowerShell 或 Windows Terminal**（本机 AI 会话的内嵌终端跑不了 mvn，已知环境问题）。

```bash
mvn spring-boot:run
```

启动后验证：

| 地址 | 预期 |
|---|---|
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/doc.html | knife4j 接口文档页 |
| http://localhost:8080/standings | `{"code":0,"message":"ok","data":[...]}` 示例积分榜 |
| http://localhost:8080/api/match/matches | 在售比赛（含 `totalStock`/`stock`） |

## 与前端联调

前端 `my-h5-app` 的 Vite 代理已把 `/api/**` 和 `/uploads/**` 转发到本服务的 8080 端口（前者去掉 `/api` 前缀，后者保持原样）。头像等静态资源经 `/uploads` 代理在开发环境可见。
开发时起两个进程：前端 `npm run dev`（5173）+ 本服务（8080）。

## 打包与运行

```bash
mvn clean package
java -jar target/my-h5-server-0.0.1-SNAPSHOT.jar
```

## 分支模型

- `main`：当前主力分支，已完成 V2 正确性加固三步（防超卖 / 幂等 / 审计）。
- `archive/v2-draft-1`：存档分支，保留过一次「V2 全量草稿（幂等 + 防超卖 + 审计）」及更早的未提交工作，仅作对照与退路，不再加功能。

## 仓库角色

本仓库是**当前主力后端**。`my-h5-api`（Node/Express 版）冻结保留，仅作对照与退路，不再加功能。
