/**
 * 应用入口 —— 全局的东西只在这里装配一次。
 *
 * ── 为什么 app.use(Vant) 全量引入 ───────────────────────
 * 全量引入最省事但会把整个组件库打进首屏（当前 ~100KB gzip）。
 * 两人项目先求快，这个体积完全可接受；
 * 将来做「构建优化」时换成按需引入（unplugin-vue-components），
 * 用到的组件才会进包，预计能砍掉一半体积。这是清单里已预留的升级路径。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import './assets/styles/index.css'

const app = createApp(App)

app.use(createPinia()) // 状态管理：各模块的 store 在 stores/ 下定义，这里只装引擎
app.use(router)
app.use(Vant)

app.mount('#app')
