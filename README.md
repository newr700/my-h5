# my-h5（英超票务 + 预测 · monorepo）

本仓库用 **git subtree** 把两个独立 git 仓库合并在一起，**各自保留完整提交历史**：

| 子目录 | 技术栈 | 内容 |
| --- | --- | --- |
| `my-h5-app/` | Vue3 + Vite + TypeScript + Element Plus + Pinia | 前端：票务（积分榜 / 购票 / 我的）+ 英超预测三页（权威解析 / AI 预测 / 历史回顾） |
| `my-h5-server/` | Spring Boot 3 + MyBatis-Plus + Flyway + JWT | 后端 API：MySQL 迁移、六维加权夺冠概率算法、专家评论等级守卫 |

子项目各自的详细说明在 `my-h5-app/README.md` 与 `my-h5-server/README.md`。

---

## 一、本地启动（从零到能打开页面）

> 整体思路：先起**后端**（Java，8080 端口，带 MySQL），再起**前端**（Node，5173 端口）。
> 前端开发服务器通过 Vite 代理把 `/api` 请求转发到本地后端，所以本地**不需要配跨域**。

### 1. 环境准备

| 工具 | 版本要求 | 用途 |
| --- | --- | --- |
| JDK | 17 | 跑后端（Spring Boot 3） |
| Maven | 3.9+ | 后端依赖管理与启动（也可用项目里的 `mvnw`） |
| Node.js | ≥ 20 | 跑前端 |
| MySQL | 8.x | 后端数据库，Flyway 启动时自动建表 |

### 2. 后端（my-h5-server）

**(a) 准备数据库** —— 新建一个本地 MySQL 库（库名、字符集按下面来）：

```sql
CREATE DATABASE my_h5 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

**(b) 配置连接** —— 后端默认读 `application-dev.yml`（主配置 `spring.profiles.active: dev` 已默认启用），
里面已经写了本机连接串、用户名 `root`、密码 `123456`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/my_h5?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

> 如果你的 MySQL 用户名 / 密码 / 端口不一样，**只改 `my-h5-server/src/main/resources/application-dev.yml` 里这三项**即可。
> 密码明文写在 dev 配置里仅用于本地开发；生产请用 `application-prod.yml` 的环境变量 `${DB_PASSWORD}`。

**(c) 启动**：

```bash
cd my-h5-server
mvn spring-boot:run
```

首次启动 Flyway 会自动执行 `src/main/resources/db/migration/` 下的 **V1~V8** 迁移脚本，
建好所有表并灌入种子数据（球队、六维评分、专家观点、历届战绩等）——**无需手动建表**。

启动成功标志（任选其一验证）：

| 地址 | 预期 |
| --- | --- |
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/standings | 示例积分榜 JSON |
| http://localhost:8080/prediction/teams | 球队夺冠概率排名 JSON |

> Maven 在部分环境的内嵌终端跑不了，用系统 **PowerShell / Windows Terminal** 执行上面的命令即可。

### 3. 前端（my-h5-app）

另开一个终端：

```bash
cd my-h5-app
npm install
npm run dev
```

- 开发服务器起在 **5173**，`/api` 和 `/uploads` 通过 `vite.config.ts` 的 `server.proxy`
  转发到本地后端 **8080**（前者去掉 `/api` 前缀，后者保持原样）。
- 打开浏览器访问 **http://localhost:5173** 即可看到页面。
- 导航栏六个入口：我的 / 购票 / 积分榜 / 权威解析 / AI 预测 / 历史回顾。
- **本地看效果只用 `npm run dev`**；不要用 `npm run preview` 连后端——
  `preview` 是纯静态预览、不继承 dev 代理，且 `.env.production` 指向占位域名 `api.example.com`，会连不上后端。

### 4. （可选）生产构建

```bash
cd my-h5-app
npm run build          # 产物在 dist/
```

构建时 `.env.production` 的 `VITE_API_BASE_URL` 会被**编译进**产物（构建时烘焙，无法运行时改）。
默认是占位域名 `https://api.example.com`，**上线前请改 `my-h5-app/.env.production` 为你真实的后端地址**，再 build。
`dist/` 是纯静态文件，用任意静态服务器（Nginx、Vercel、对象存储等）托管即可。

---

## 二、目录结构与同步

- `my-h5-app/`：前端（详见 `my-h5-app/README.md`）
- `my-h5-server/`：后端（详见 `my-h5-server/README.md`）

本仓库由 subtree 合并而来，两个子目录各自保留独立提交历史。
如需把子项目的新提交同步进本仓库（以 `my-h5-app` 为例）：

```bash
# 先在各子项目里提交，再在仓库根目录执行：
git subtree pull --prefix=my-h5-app <前端仓库路径或远程地址> main
```

---

## ⚠️ 安全提醒

`my-h5-server/src/main/resources/application-dev.yml` 内含**本地数据库明文密码（123456）**，
且本仓库当前为 **Public**。

- 如果你当私有项目用：请保持 Private，不要公开。
- 既然已公开：**不要在你自己的生产 / 重要 MySQL 上复用这个密码**；克隆后把 `application-dev.yml` 的密码
  改成你本地库的真实密码即可，本地开发不受影响。
- 生产配置 `application-prod.yml` 已改用环境变量 `${DB_PASSWORD}`，不落地密码。
