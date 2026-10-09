import { createRouter, createWebHistory } from 'vue-router'
import userRoutes from './modules/user'
import standingsRoutes from './modules/standings'
import analysisRoutes from './modules/analysis'
import predictionRoutes from './modules/prediction'
import historyRoutes from './modules/history'

/**
 * 路由总表 —— 各模块的路由在 modules/ 下各自维护。
 *
 * ── 为什么要拆 modules/ ────────────────────────────────
 * 两人协作时如果所有路由都写在这一个文件里，谁加页面谁改它，
 * 天天冲突。拆开后各管各的，这个文件基本不再变动。
 * 新增模块三步：modules/ 下建文件 → 这里 import → 数组里展开。
 *
 * ── 为什么用 history 模式（地址干净，无 #）──────────────
 * 与 hash 模式对比：hash 靠 location.hash 不发请求，静态服务器零配置即可；
 * history 靠 pushState，URL 更优雅（/standings 而非 /#/standings），
 * 但代价是【部署必须配置 SPA fallback】：任意子路径刷新或直链都要返回 index.html，
 * 否则 404。本项目已在根目录 vercel.json 配了 rewrites 兜底（Vercel 平台）。
 * 若换 Nginx，加一句 `try_files $uri $uri/ /index.html;` 同理。
 *
 * （技能点：vue-router 双模式原理——hash 不发请求 vs history 需 server 配合；
 *  面试高频对比题，重点讲清“history 模式上线为什么要多配一步”）
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/user' },
    ...userRoutes,
    ...standingsRoutes,
    ...analysisRoutes,
    ...predictionRoutes,
    ...historyRoutes
  ]
})

// ── 路由守卫：前端的第一道登录拦截 ─────────────────────────
// 没登录却访问 requiresAuth 页面 → 送去 /login，并把「原本想去哪」
// 存在 ?redirect= 里，登录成功后原路送回（见 LoginPage.vue 的 goBack）。
//
// 注意这只是【体验层】的拦截：守卫拦不住直接 curl 打后端接口的人 ——
// 真正的防线在 Java 端 AuthInterceptor。前端的锁防君子，后端的锁防所有人，
// 这又是「前端校验为体验，后端校验为安全」的一个实例。
router.beforeEach((to) => {
  const hasToken = !!localStorage.getItem('token')
  if (to.meta.requiresAuth && !hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录还访问登录页 → 没必要，直接回用户中心
  if (to.path === '/login' && hasToken) {
    return { path: '/user' }
  }
  return true
})

// 每次路由切换后同步浏览器标签页标题——小细节，但专业感就藏在这些地方
router.afterEach((to) => {
  document.title = (to.meta.title as string) || 'my-h5-app'
})

export default router
