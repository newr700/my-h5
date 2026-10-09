import type { RouteRecordRaw } from 'vue-router'

/**
 * 权威解析模块路由（页面2）
 *
 * ── 为什么不加 requiresAuth ──────────────────
 * 页面接口是公开读接口（后端没把 /prediction/** 整段拦起来），
 * 逛网站的人不该为了看眼专家解析就被迫登录。
 * 只有「发表评论」这一个写操作需要登录，由后端接口自身拦截（401 / 6002），
 * 页面输入框只是对未登录/非专家隐藏。
 *
 * ── 懒加载 ──────────────────────────────────
 * component: () => import(...)：页面代码单独打包，进路由才下载。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/analysis',
    name: 'AnalysisBoard',
    component: () => import('@/views/analysis/AnalysisBoard.vue'),
    meta: { title: '权威解析' }
  }
]

export default routes
