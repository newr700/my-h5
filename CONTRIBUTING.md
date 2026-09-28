# 协作约定（开工前必读）

## 分支模型（简化 GitHub Flow）

- `main` 永远可发布，**禁止直接提交**，只接受 PR 合并。
- 日常开发分支：`feature/模块-功能`（新功能）、`fix/问题描述`（修 bug）。
- 分支寿命 ≤ 3 天，做完就合，合完就删。

## 每日流程口诀

开工先 pull，做完提 PR，review 过才合，合完删分支。

```bash
git checkout main && git pull        # 每天开工先同步
git checkout -b feature/user-login   # 从最新 main 开分支
# …写代码，小步提交…
git push -u origin feature/user-login
# 到 GitHub/Gitee 发 PR，@对方 review，通过后合并并删除分支
```

## Commit 规范（Conventional Commits）

格式：`类型: 一句话说明`（中文可以，动词开头）

- `feat: 新增登录页`
- `fix: 修复订单列表分页越界`
- `refactor: 抽离请求拦截器`
- `style: 调整首页间距`（纯格式，不改逻辑）
- `docs: 更新接口说明`
- `chore: 升级依赖`

## 目录责任分区

| 区域 | 位置 | 规则 |
| --- | --- | --- |
| 共享区 | `api/` `assets/` `components/` `composables/` `router/` `stores/` `types/` `utils/` | 改任何文件前，先在群里说一声 |
| 业务区 | `views/模块/` + 对应的 `api/模块.ts` `stores/模块.ts` `router/modules/模块.ts` | 认领人自己说了算 |

新文件放哪的判断标准：**被 ≥2 个模块使用 → 共享区；只属于一个页面 → 就近放模块里。**

## 依赖方向（不许违反）

`views → stores → api → utils`，只能向下调用：

- 页面里禁止直接写 axios，必须走 `stores`。
- `utils` / `types` 不依赖任何上层目录。
- 后端字段变更先改 `src/types/api.ts`，再让 TS 报错带你找出所有要改的地方。

## 代码风格

机器管，人不吵：

```bash
npm run lint      # ESLint 检查
npm run format    # Prettier 一键格式化
npm run type-check  # TS 类型检查
```

提交前跑一遍，红了别提交。

## 环境约定

- Node ≥ 20，包管理用 npm（`package-lock.json` 必须提交，保证两人依赖一致）。
- 接口地址只写在 `.env.*` 里，代码里禁止出现硬编码域名。
- 真实密钥（如以后接第三方服务）只放 `.env.local`，永不入库。
