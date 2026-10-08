# my-h5-app

双人协作的 Web 项目，定位为 **PC 浏览为主、兼顾手机**。前端基于 Vue 3 + Vite + TypeScript，后端为独立仓库 `my-h5-server`（Java）。

## 快速开始

```bash
npm install
npm run dev
npm run build
npm run preview
```

> ⚠️ 每条命令单独复制执行即可，**不要连带 `#` 后面的中文**——终端里 `#` 是注释符，连带复制会被当成 npm 的包名而报 `EINVALIDTAGNAME`（之前有人踩过这个坑）。

要求 Node ≥ 20。开发时接口通过 Vite 代理转发到本地后端（默认 `http://localhost:8080`，见 `vite.config.ts`）。

## 技术栈

| 用途 | 选型 |
| --- | --- |
| 框架 | Vue 3 + Vite + TypeScript |
| UI 组件库 | **Element Plus 2.14.7（桌面组件库，PC 为主）** |
| 状态管理 | Pinia |
| 路由 | Vue Router（**history 模式**，按模块拆分） |
| 请求 | Axios（统一封装于 `src/api/request.ts`） |
| 响应式 | 自有 `responsive.css`（媒体查询，768px 断点；PC 铺满全屏，手机内边距收窄） |
| 代码规范 | ESLint + Prettier |

> 演进说明：早期按「移动端为主」用 Vant + postcss-px-to-viewport（px 转 vw）；2026-10-05 起改为 PC 为主，切换 Element Plus、移除 px-to-viewport，详见各文件顶部注释。

## 双端策略（PC 为主、兼顾手机）

- **桌面铺满全屏**：`responsive.css` 让导航栏与内容在任意宽度下都铺满视口，不再限宽居中。
- **内容区内边距**：默认 24px，`< 768px` 手机收窄到 12px，防贴边。
- 只用一个断点是刻意取舍：主战场在 PC，手机是附加形态，每多一个断点布局分支是乘法关系。

## 顶部导航栏

`src/App.vue` 内置 Element Plus 横向 `el-menu`，做成「常规 Web 导航栏」：

- 左侧：品牌名 `my-h5-app`
- 中间：我的 / 购票 / 积分榜（router 模式，当前页自动高亮）
- 追加三个预测入口：权威解析 / AI 预测 / 历史回顾（见下方「英超预测模块」一章）
- 右侧：登录态 —— 已登录显示头像 + 用户名下拉（可退出），未登录显示「未登录」按钮
- 登录页 `meta.nav === false` 时不显示导航栏（做全屏登录页）

> 六个入口较多时，`el-menu` 设了 `:ellipsis="false"`，宽度不足会换行而不是折叠，
> 目的是保证所有入口始终可见（宁可占两行，也不让用户猜菜单里藏了什么）。

## 路由与登录守卫

- 使用 **history 模式**（URL 干净无 `#`），代价是部署必须配 SPA fallback：本项目已在根目录 `vercel.json` 用 `rewrites` 兜底任意子路径刷新 / 直链；换 Nginx 加 `try_files $uri $uri/ /index.html;` 同理。
- `router.beforeEach` 做前端登录拦截（体验层）：未登录访问受保护页跳 `/login` 并带 `?redirect=`；真正的防线在 Java 端 `AuthInterceptor`，前端锁防君子、后端锁防所有人。

## 英超预测模块（权威解析 / AI 预测 / 历史回顾）

在导航栏追加的三个入口，对应三张 $[\text{草图}]$ 页面。数据来源是**后端数据库真表**，不是写死的前端假数据——
新增/修改球队、赛季、专家观点都在 MySQL 里改，改完刷新页面即可生效。

### 实现状态（以代码为准，别只看口述）

| 部分 | 状态 | 说明 |
| --- | --- | --- |
| 数据表（Flyway V7/V8） | ✅ 完成 | MySQL 与 H2 双环境迁移均通过 |
| 后端接口（7 个） | ✅ 完成 | 5 个只读接口已 curl 验证，见下方「接口一览」 |
| 夺冠概率算法 | ✅ 完成 | 整数加权求和，见「算法怎么来的」 |
| 前端三个页面 | ✅ 完成 | `src/views/prediction/` 三页 + 两个组件 |
| 导航栏三个入口 | ✅ 完成 | `App.vue` 追加三项，窄屏自动换行 |
| 类型检查 | ✅ 通过 | `npm run type-check`（vue-tsc）零错误 |
| 评论写接口三态 | ✅ 已联调验证 | 2026-10-08 跑通：未登录 1101 / 普通用户 6002 / 专家成功落库 |

> 结论：**前后端已打通，三个界面可以打开看，发表评论权限也已验证。**

### 接口一览

统一挂在 `/prediction` 下，全部返回 `Result` 外壳 `{code, message, data}`：

| 方法 | 路径 | 用途 | 登录 |
| --- | --- | --- | --- |
| GET | `/prediction/analysis` | 专家观点列表（含各自评论条数） | 否 |
| GET | `/prediction/analysis/{id}/comments` | 某条解析的评论 | 否 |
| POST | `/prediction/analysis/{id}/comment` | **发表评论** | **是，且需 Lv.2** |
| GET | `/prediction/teams` | 球队夺冠概率排名（含六维分） | 否 |
| GET | `/prediction/algorithm` | 六维权重说明 | 否 |
| GET | `/prediction/history` | 历届战绩（冠亚季军） | 否 |
| GET | `/prediction/history/titles` | 2000 年以来夺冠次数 Top5 | 否 |

只有写评论这一条路径在 `AuthInterceptor` 保护名单里（`/prediction/analysis/*/comment`）。
**刻意不拦整个 `/prediction/**`** —— 为一个写接口把三个读接口一起关掉，未登录的人就什么都看不到了。

### 一、权威解析（`/analysis`）

- **中/英切换**：专家姓名、支持理由都有中英两版，切换开关只改展示语言，不重新请求。
- **球队配色标记**：每位专家支持哪支队，卡片就用该队的主/辅色描边或标色（草图批注「专家可以选择颜色来标记（用球队颜色）」）。
  颜色不写在前端，统一由 `football_team` 字典表下发——避免同一个红色在三处各抄一遍。
- **评论**：卡片上显示已有评论条数；展开发评论。
  **只有「行业专家（Lv.2）」能发**（草图批注「高等级用户可在这里写评论」）。
  前端会把输入框藏起来，但那只是体验——真正拦人是后端的等级守卫。

### 二、AI 预测（`/teams` + `/algorithm`）

草图的核心诉求：「这个表有 6 维」「需要排序，需要写预测算法」「这个有夺冠概率」。

**算法怎么来的**（`PredictionScoring`，整数运算，不碰浮点）：

| 维度 key | 含义 | 权重 |
| --- | --- | --- |
| history | 历史夺冠次数 | 15 |
| star | 明星球员 | 20 |
| homeAway | 主客优势 | 10 |
| tactic | 战术分析 | 20 |
| matchup | 对位优势 | 15 |
| squad | 阵容厚度 | 20 |

```
夺冠概率 = round( (Σ(维度分 × 权重) + 50) / 100 )
```

权重合计正好 100，每个维度分 0~100，所以结果天然落在 0~100（再 `clamp` 兜一道）。
全程整数运算是为了避开浮点累加误差——`0.15 + 0.2 + …` 在多个数累加后可能差 1，
而这里权重是整数、`long` 累加，结果与手算必然一致。

**概率不落库**：库里只有六维原始分，概率每次由接口现算。
存一份「算好的概率」等于存了两份互相可能打架的真相（和积分榜不存 `rank` 同理）。
`rank` 也由后端在排序后生成，前端不参与。

- **雷达图**：每支队的六边形雷达图展示六维；草图批注「要可调数字显示」，即数值可在图上开关显示。
- **排序**：按概率倒序，同概率时用队名兜底，保证顺序稳定（不会出现每次刷新排名乱跳）。
- **算法说明**：页面底部展示权重表，数据来自 `/algorithm` 接口——值就是计算用的那套权重，
  不会出现「说明写着 20、代码里却是 15」的漂移。

### 三、历史回顾（`/history` + `/history/titles`）

- **历届战绩**：届数 / 赛季（如 `2024-25`）/ 冠军 / 亚军 / 季军，按届数倒序。
  赛季标签 `2000-01` 这种形式由后端用「年份 + 年份+1」拼出来，不存成两个字段。
- **2000 年以来夺冠次数 Top5**：**现算**（`GROUP BY champion_team` + `COUNT`），
  不另建统计表。所以以后追加新赛季，这个榜自动更新，不需要有人记得同步维护。
  当前数据：曼城 8 / 曼联 7 / 切尔西 5 / 利物浦 2 / 阿森纳 2。

### 数据表（V7 建表、V8 种子数据）

| 表 | 作用 |
| --- | --- |
| `football_team` | 球队字典：中英文名、三字母缩写、主辅色、队徽 |
| `team_prediction` | 六维评分（**不含**概率列，见上文） |
| `team_star_player` | 明星球员（号码、位置、照片） |
| `expert_analysis` | 专家观点：姓名中英、支持球队、理由 |
| `expert_comment` | 评论：存**发布时的昵称与等级快照** |
| `season_history` | 历届战绩：届数、赛季年份、冠亚季军 |

**评论为什么存快照而不是 JOIN 用户表**：等级是「当时他有资格说这句话」的证据，
事后降级不该改写历史评论的等级标记——和订单里存下单时的单价是同一个道理。

### 前端在哪

| 文件 | 作用 |
| --- | --- |
| `src/views/prediction/AnalysisBoard.vue` | 页面2 权威解析：中/英切换、专家卡片、评论 |
| `src/views/prediction/AiPrediction.vue` | 页面4 AI 预测：六维表、雷达图、概率、算法说明 |
| `src/views/prediction/HistoryReview.vue` | 页面5 历史回顾：历届战绩 + 夺冠次数 Top5 |
| `src/views/prediction/components/RadarChart.vue` | 手写 SVG 六边形雷达图（数值可开关显示） |
| `src/components/TeamCrest.vue` | 队徽 —— 三页共用所以放全局区：有图用图，没图则用球队配色 + 缩写画盾牌 |
| `src/api/prediction.ts` | 接口层，沿用 `standings.ts` 的 `get<unknown>` + parse 校验 |
| `src/stores/prediction.ts` | 三个 store，统一处理加载 / 刷新 / 失败三态 |
| `src/router/modules/prediction.ts` | 路由 `/analysis`、`/prediction`、`/history`（懒加载） |

三个路由**刻意都不加 `requiresAuth`**：看预测不该被迫登录。
需要凭证的只有「写评论」这一个动作，而它由后端接口自己拦。

> 判断新组件放哪的原则照旧：**被 ≥2 个页面用 → 全局 `src/components/`；只服务一个页面 → 就近放模块的 `components/`。**
> `TeamCrest` 三页都用所以进全局，`RadarChart` 只有 AI 预测用所以留在页内。

### 如何验证

后端起在 8080 后，只读接口可以直接查：

```bash
curl -s http://localhost:8080/prediction/analysis
curl -s http://localhost:8080/prediction/teams
curl -s http://localhost:8080/prediction/algorithm
curl -s http://localhost:8080/prediction/history
curl -s http://localhost:8080/prediction/history/titles
```

「发表评论」的权限必须**三件事同时成立**才算通过 —— 只测「专家能发」等于没验证权限功能，
因为它的价值恰恰在于把不该放行的人拦住：

| 场景 | 期望结果 |
| --- | --- |
| 未登录 POST `/prediction/analysis/1/comment` | `401`（连门都进不来） |
| 已登录的普通用户（注册默认 Lv.1） | `6002`（进门了，但等级不够） |
| 调 `POST /user/expert-apply` 升级后再发 | `0` 成功，评论落库 |

### 踩过的坑（留给后来人）

- `Invalid bound statement (not found): ...SeasonHistoryMapper.selectTitleCounts`
  → Mapper 聚合方法**漏写 `@Select`**。编译期不报错，要调用才炸，
  而且被 `Result` 外壳包成 `{"code":5000}`，很容易被误判成「服务端抽风」。
  记住：这条报错几乎总是「SQL 忘了绑」，不是数据库坏了。
- 评论权限必须在后端判。只靠前端藏输入框，改一行 JS 就能绕过。

## 目录结构

```
src/
├── api/           # 所有后端请求，按模块分文件（request.ts 为统一封装）
├── assets/        # 全局样式（index.css 基础 / responsive.css 响应式）、图片、字体
├── components/    # 全局通用组件（≥2 个模块使用才放这里）
├── composables/   # 全局组合式函数（useXxx）
├── router/        # 路由总表 + modules/ 按模块拆分
├── stores/        # Pinia 状态，按模块分文件
├── types/         # TS 类型，与后端接口字段对齐
├── utils/         # 纯工具函数
├── views/         # 页面，按业务模块分目录（user/ order/ standings/ …）
└── App.vue        # 全局外壳：顶部导航栏 + 内容区
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

## 头像上传

- 后端提供 `POST /api/user/avatar`（字段名 `file`，仅 JPG/PNG/WebP，≤2MB），图片存到后端磁盘、DB 存相对路径 `/uploads/xxx.png`，由后端映射为可访问 URL。
- 前端入口在「用户中心」页（`src/views/user/UserHome.vue`）的「更换头像」按钮：用原生 `<input type="file">` 选图，经自封装 axios（`src/api/user.ts` 的 `uploadAvatar`）上传——**不用 el-upload 自带 XHR**，因为它会绕过 token 拦截器与统一错误处理。
- 头像显示：`src/utils/avatar.ts` 的 `avatarSrc()` 把后端相对路径按部署环境拼成可访问地址（dev 靠 Vite 的 `/uploads` 代理，prod 拼成绝对域名）；右上角（`App.vue`）与用户中心都优先显示图片，未设置时显示昵称首字母占位。
- 开发时头像文件经 Vite 的 `/uploads` 代理转发到后端 8080（见 `vite.config.ts`）。

## 与后端联调

- 本地后端主力是 **Java（my-h5-server，8080）**；Node 版 `my-h5-api`（3000）冻结保留作对照。
- `vite.config.ts` 的 `server.proxy` 把 `/api` 转发到 `http://localhost:8080` 并去掉前缀（后端不配 context-path，前缀是代理层职责）；同时把 `/uploads` 转发到 8080（不去掉前缀），供开发环境访问头像等静态资源。
- 生产环境跨域走后端 CORS，或前后端同域部署。

## 协作

分支模型、commit 规范、责任分区、依赖方向红线，全部见 [CONTRIBUTING.md](./CONTRIBUTING.md)。开工前必读。

## 常用命令

```bash
npm run lint         # ESLint 检查
npm run format       # Prettier 一键格式化
npm run type-check   # TS 类型检查（vue-tsc --noEmit）
```
