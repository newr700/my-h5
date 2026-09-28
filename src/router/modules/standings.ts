import type { RouteRecordRaw } from 'vue-router'

/** 积分榜模块路由（样例模块，供参考模仿） */
const routes: RouteRecordRaw[] = [
  {
    path: '/standings',
    name: 'StandingsList',
    component: () => import('@/views/standings/StandingsList.vue'),
    meta: { title: '当前积分排名' }
  }
]

export default routes
