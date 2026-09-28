import { createRouter, createWebHashHistory } from 'vue-router'
import userRoutes from './modules/user'
import orderRoutes from './modules/order'

/**
 * 路由总表 —— 各模块的路由在 modules/ 下各自维护，
 * 这里只做汇总，避免多人同时改一个文件造成冲突。
 * 用 hash 模式：H5 部署到任何静态服务器都不需要额外配置。
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/user' },
    ...userRoutes,
    ...orderRoutes
  ]
})

router.afterEach((to) => {
  document.title = (to.meta.title as string) || 'my-h5-app'
})

export default router
