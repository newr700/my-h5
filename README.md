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

## 快速开始

**用 Windows 自带的 PowerShell 或 Windows Terminal**（本机 AI 会话的内嵌终端跑不了 mvn，已知环境问题）。

```bash
mvn spring-boot:run
```

启动后验证三处：

| 地址 | 预期 |
|---|---|
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/doc.html | knife4j 接口文档页 |
| http://localhost:8080/standings | `{"code":0,"message":"ok","data":[...]}` 示例积分榜 |

## 与前端联调

前端 `my-h5-app` 的 Vite 代理已把 `/api/**` 转发到本服务的 8080 端口（去掉 `/api` 前缀）。
开发时起两个进程：前端 `npm run dev`（5173）+ 本服务（8080）。

## 打包与运行

```bash
mvn clean package
java -jar target/my-h5-server-0.0.1-SNAPSHOT.jar
```

## 仓库角色

本仓库是**当前主力后端**。`my-h5-api`（Node/Express 版）冻结保留，仅作对照与退路，不再加功能。
