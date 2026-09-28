import type { RouteRecordRaw } from 'vue-router'

/** 用户模块路由（码农 A 维护） */
const routes: RouteRecordRaw[] = [
  {
    path: '/user',
    name: 'UserHome',
    component: () => import('@/views/user/UserHome.vue'),
    meta: { title: '用户中心' }
  }
]

export default routes
