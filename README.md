# my-h5（英超票务 + 预测 · monorepo）

本仓库用 **git subtree** 把两个独立 git 仓库合并在一起，**各自保留完整提交历史**：

| 子目录 | 技术栈 | 内容 |
| --- | --- | --- |
| `my-h5-app/` | Vue3 + Vite + TypeScript + Element Plus + Pinia | 前端：票务（积分榜 / 购票 / 我的）+ 英超预测三页（权威解析 / AI 预测 / 历史回顾） |
| `my-h5-server/` | Spring Boot 3 + MyBatis-Plus + Flyway + JWT | 后端 API：MySQL 迁移、六维加权夺冠概率算法、专家评论等级守卫 |

## 本地运行

### 后端
详见 `my-h5-server/README.md`。需要本机 MySQL 8，Flyway 启动时自动建表（V1~V8）。默认端口 8080。

### 前端
详见 `my-h5-app/README.md`。需先起后端（8080），再到 `my-h5-app` 目录：

```bash
npm install
npm run dev      # 起在 5173，/api 代理到后端 8080
```

## ⚠️ 安全提醒

`my-h5-server/src/main/resources/application-dev.yml` 内含**本地数据库明文密码**。
**本仓库请保持 Private（私有）**，不要设为公开，避免密码泄露。
生产配置 `application-prod.yml` 已改用环境变量 `${DB_PASSWORD}`，不落地密码。
