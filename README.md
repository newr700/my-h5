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

## 协作

分支模型、commit 规范、责任分区、依赖方向红线，全部见 [CONTRIBUTING.md](./CONTRIBUTING.md)。开工前必读。

## 常用命令

```bash
npm run lint         # ESLint 检查
npm run format       # Prettier 一键格式化
npm run type-check   # TS 类型检查
```
