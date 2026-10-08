import type { RouteRecordRaw } from 'vue-router'

/** 用户模块路由（码农 A 维护） */
const routes: RouteRecordRaw[] = [
  {
    path: '/user',
    name: 'UserHome',
    component: () => import('@/views/user/UserHome.vue'),
    // requiresAuth：路由守卫（router/index.ts）凭这个标记决定要不要拦。
    // 标记加在路由上而不是写死在守卫的名单里 —— 新页面自己声明「我要登录」，
    // 守卫代码不用跟着改（开放封闭原则的一个小例子）
    meta: { title: '用户中心', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    // nav: false —— 登录页做全屏页，App.vue 据此不渲染顶部导航栏
    meta: { title: '登录', nav: false }
  }
]

export default routes
