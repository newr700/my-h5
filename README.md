# my-h5-app

双人协作的移动端 H5 项目。前端基于 Vue 3 + Vite + TypeScript，后端为独立仓库（另行搭建）。

## 快速开始

```bash
npm install        # 首次克隆后安装依赖
npm run dev        # 起开发服务器
npm run build      # 类型检查 + 生产打包
npm run preview    # 本地预览打包产物
```

要求 Node ≥ 20。开发时接口通过 Vite 代理转发到本地后端（默认 `http://localhost:3000`，见 `vite.config.ts`）。

## 技术栈

| 用途 | 选型 |
| --- | --- |
| 框架 | Vue 3 + Vite + TypeScript |
| UI 组件库 | Vant（移动端） |
| 状态管理 | Pinia |
| 路由 | Vue Router（hash 模式，按模块拆分） |
| 请求 | Axios（统一封装于 `src/api/request.ts`） |
| 移动端适配 | postcss-px-to-viewport（按 375 设计稿写 px，自动转 vw） |
| 代码规范 | ESLint + Prettier |

## 目录结构

```
src/
├── api/           # 所有后端请求，按模块分文件（request.ts 为统一封装）
├── assets/        # 全局样式、图片、字体
├── components/    # 全局通用组件（≥2 个模块使用才放这里）
├── composables/   # 全局组合式函数（useXxx）
├── router/        # 路由总表 + modules/ 按模块拆分
├── stores/        # Pinia 状态，按模块分文件
├── types/         # TS 类型，与后端接口字段对齐
├── utils/         # 纯工具函数
└── views/         # 页面，按业务模块分目录（user/ order/ …）
```

判断新文件放哪：**被 ≥2 个模块使用 → 共享区；只属于一个页面 → 就近放模块里。**

## 新手指引：我要写代码，该放哪、怎么放

记住一句话：**所有代码都写在 `src/` 里，`src/` 之外的东西（配置文件）不要动。** 对照下面的场景查：

### 场景速查表

| 我要写的东西 | 放在哪 | 说明 |
| --- | --- | --- |
| 一个新页面 | `src/views/模块名/页面名.vue` | 比如 `src/views/user/Login.vue` |
| 页面里调后端接口 | `src/api/模块名.ts` 里加一个函数 | 页面**不许**自己写 axios |
| 页面要存的数据/状态 | `src/stores/模块名.ts` | 页面通过 store 拿数据，不直接调 api |
| 让页面能通过网址访问 | `src/router/modules/模块名.ts` 里加一条路由 | 不加路由，页面写了也打不开 |
| 只有这个页面用的小组件 | `src/views/模块名/components/xxx.vue` | 就近放，别污染全局 |
| 两个以上页面都要用的组件 | `src/components/xxx.vue` | 全局共享区，改动前群里说一声 |
| 这个页面独有的样式 | 写在 `.vue` 文件底部的 `<style scoped>` 里 | 有 `scoped` 就不会影响别人 |
| 全项目都要的样式/主题色 | `src/assets/styles/index.css` | 全局唯一入口 |
| 图片、图标 | `src/assets/images/` | 代码里用 `@/assets/images/xx.png` 引用 |
| 后端返回数据的类型 | `src/types/api.ts` 里加一个 interface | 先写类型再写逻辑，TS 会保护你 |
| 一个纯函数（如格式化日期） | `src/utils/xxx.ts` | 不碰接口、不碰页面的才放这里 |
| 接口地址、环境变量 | `.env.development` / `.env.production` | 代码里禁止写死域名 |

### 完整示例：新增一个「消息」模块需要建几个文件？

假设你要做一个消息列表页，一共动 **4 个文件**，缺一个都跑不通：

```
① src/views/message/MessageList.vue     ← 页面本身（你主要写逻辑的地方）
② src/api/message.ts                    ← 调接口的函数，比如 fetchMessageList()
③ src/stores/message.ts                 ← 存消息列表数据，页面从这里取
④ src/router/modules/message.ts         ← 注册路由 /message，然后在 router/index.ts 里 ...messageRoutes
```

调用链永远是：**页面(①) → 状态(③) → 接口(②) → 后端**。参考现成的 `user` 模块，四个文件抄着改名字就行。

### 三个「不许」（违反会被 review 打回）

1. **不许**在 `.vue` 页面里直接 `import axios` —— 请求必须走 `src/api/`。
2. **不许**改别人的模块目录 —— `user/` 是 A 的，`order/` 是 B 的，各扫门前雪。
3. **不许**把密钥、密码写进任何代码文件 —— 只能放 `.env.local`（这个文件 git 不会提交）。

## 协作

分支模型、commit 规范、责任分区、依赖方向红线，全部见 [CONTRIBUTING.md](./CONTRIBUTING.md)。开工前必读。

## 常用命令

```bash
npm run lint         # ESLint 检查
npm run format       # Prettier 一键格式化
npm run type-check   # TS 类型检查
```
