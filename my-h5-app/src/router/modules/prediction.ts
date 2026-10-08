import type { RouteRecordRaw } from 'vue-router'

/**
 * 英超预测模块路由（草图三张页面）
 *
 * ── 为什么三个页面都【不加 requiresAuth】 ──────────────────
 * 这三张页面的接口全是公开读接口（后端刻意没把 /prediction/** 整段拦起来），
 * 逛网站的人不该为了看一眼夺冠预测就被迫登录。
 * 只有「发表评论」这一个写操作需要登录，而它是由后端接口自身拦截的（返回 401 / 6002），
 * 页面上的输入框只是对未登录/非专家隐藏——不需要把整个页面锁起来。
 *
 * ── 懒加载 ──────────────────────────────────────────────
 * 一律 component: () => import(...)：页面代码单独打包，
 * 进到该路由才下载，首屏不背这三个页面的体积。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/analysis',
    name: 'AnalysisBoard',
    component: () => import('@/views/prediction/AnalysisBoard.vue'),
    meta: { title: '权威解析' }
  },
  {
    path: '/prediction',
    name: 'AiPrediction',
    component: () => import('@/views/prediction/AiPrediction.vue'),
    meta: { title: 'AI 预测' }
  },
  {
    path: '/history',
    name: 'HistoryReview',
    component: () => import('@/views/prediction/HistoryReview.vue'),
    meta: { title: '历史回顾' }
  }
]

export default routes
