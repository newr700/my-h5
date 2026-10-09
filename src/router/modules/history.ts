import type { RouteRecordRaw } from 'vue-router'

/**
 * 历史回顾模块路由（页面5）
 *
 * ── 接口均为公开读接口，无需登录 ──────────────
 * 历届战绩与夺冠次数都是公开数据，没理由锁起来。
 *
 * ── 懒加载 ──────────────────────────────────
 * component: () => import(...)：页面代码单独打包，进路由才下载。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/history',
    name: 'HistoryReview',
    component: () => import('@/views/history/HistoryReview.vue'),
    meta: { title: '历史回顾' }
  }
]

export default routes
