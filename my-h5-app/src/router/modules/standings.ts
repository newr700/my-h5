import type { RouteRecordRaw } from 'vue-router'

/** 积分榜模块路由（样例模块，供参考模仿） */
const routes: RouteRecordRaw[] = [
  {
    path: '/standings',
    name: 'StandingsList',
    // () => import() 动态导入 = 清单「路由懒加载」：
    // 页面代码单独打包，用户进这个路由时才下载，首屏体积不背所有页面的锅
    // （技能点：代码分割 Code Splitting——面试必问“首屏优化你做过什么”，这是标准答案第一条）
    component: () => import('@/views/standings/StandingsList.vue'),
    meta: { title: '当前积分排名' }
  }
]

export default routes
