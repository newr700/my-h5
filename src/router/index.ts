import { createRouter, createWebHashHistory } from 'vue-router'
import userRoutes from './modules/user'
import orderRoutes from './modules/order'
import standingsRoutes from './modules/standings'

/**
 * 路由总表 —— 各模块的路由在 modules/ 下各自维护。
 *
 * ── 为什么要拆 modules/ ────────────────────────────────
 * 两人协作时如果所有路由都写在这一个文件里，谁加页面谁改它，
 * 天天冲突。拆开后各管各的，这个文件基本不再变动。
 * 新增模块三步：modules/ 下建文件 → 这里 import → 数组里展开。
 *
 * ── 为什么用 hash 模式（地址里带 #）────────────────────
 * 部署时只需要一个静态文件服务器，不需要任何“路径重写”配置；
 * 代价是 URL 不够优雅（/#/standings）。
 * 若将来要改成 history 模式获得干净 URL，
 * 服务器必须补一条“所有路径都返回 index.html”的重写规则，别忘了。
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/user' },
    ...userRoutes,
    ...orderRoutes,
    ...standingsRoutes
  ]
})

// 每次路由切换后同步浏览器标签页标题——小细节，但专业感就藏在这些地方
router.afterEach((to) => {
  document.title = (to.meta.title as string) || 'my-h5-app'
})

export default router
