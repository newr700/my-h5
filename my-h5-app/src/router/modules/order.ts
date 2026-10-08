import type { RouteRecordRaw } from 'vue-router'

/** 订单模块路由（码农 B 维护） */
const routes: RouteRecordRaw[] = [
  {
    path: '/order',
    name: 'OrderHome',
    component: () => import('@/views/order/OrderHome.vue'),
    // 下单/看订单需要登录（PRD 5.2）—— 未登录访问会被守卫送去 /login
    meta: { title: '订单', requiresAuth: true }
  }
]

export default routes
