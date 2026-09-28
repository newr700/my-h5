import type { RouteRecordRaw } from 'vue-router'

/** 订单模块路由（码农 B 维护） */
const routes: RouteRecordRaw[] = [
  {
    path: '/order',
    name: 'OrderHome',
    component: () => import('@/views/order/OrderHome.vue'),
    meta: { title: '订单列表' }
  }
]

export default routes
