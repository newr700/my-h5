# my-h5-server

`my-h5-app`（Vue3 前端）的 Java 后端。技术栈：Java 17 + Spring Boot 3.5 + Maven。

> 需求与技术裁定见前端仓库 `my-h5-app/docs/`：
> 《PRD-项目需求文档》《工程实施手册》《后端架构与上线全流程路线》。**先读文档再动手。**

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
