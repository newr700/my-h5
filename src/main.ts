/**
 * 应用入口 —— 全局的东西只在这里装配一次。
 *
 * ── 为什么 app.use(Vant) 全量引入 ───────────────────────
 * 全量引入最省事但会把整个组件库打进首屏（当前 ~100KB gzip）。
 * 两人项目先求快，这个体积完全可接受；
 * 将来做「构建优化」时换成按需引入（unplugin-vue-components），
 * 用到的组件才会进包，预计能砍掉一半体积。这是清单里已预留的升级路径。
 *
 * （技能点：应用入口与插件机制——app.use() 背后就是 Vue 的插件规范，
 *  一个带 install 方法的对象；面试问"Vue 插件怎么写"就答这里）
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
// 样式加载顺序是有意义的，别随手调换：
//   1) vant 组件库样式（第三方，最先）
//   2) index.css      本项目基础样式（reset + 主题变量），要能盖住组件库默认值
//   3) responsive.css PC 端断点规则，要能盖住基础样式里的移动端设定
// CSS 同级选择器「后加载者胜」，顺序错了覆盖就不生效，而且这种 bug 极难排查。
import './assets/styles/index.css'
import './assets/styles/responsive.css'

const app = createApp(App)

app.use(createPinia()) // 状态管理：各模块的 store 在 stores/ 下定义，这里只装引擎
app.use(router)
app.use(Vant)

app.mount('#app')
