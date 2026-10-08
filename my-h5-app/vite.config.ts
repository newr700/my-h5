/**
 * Vite 构建配置 —— 本文件是「工程化」技能点的聚集地。
 *
 * ── 面试技能点（详见 docs/面试技能树.md T3/T4/T5）────────
 * 路径别名（resolve.alias）/ 开发代理（server.proxy，解决跨域）
 * 考点速记：为什么 Vite 快？（esbuild 预构建 + 按需编译，不用先打包整个项目）
 *
 * ── 2026-10-05 重大变更：从「移动端 vw 适配」切到「Element Plus 桌面组件库」──
 * 之前用 postcss-px-to-viewport 把 px 转 vw，是按「手机为主」设计的；
 * 现在前端定位改为「PC 浏览为主、兼顾手机」，改用 Element Plus（桌面组件库），
 * 页面直接写真实像素，不再等比缩放。因此已移除该 PostCSS 插件。
 * 响应式改由本项目的 responsive.css 用媒体查询做「限宽居中」处理。
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 固定开发端口为 5173（显式声明，避免被占用时 Vite 自动跳到 5174）
    port: 5173,
    strictPort: false,
    proxy: {
      // 开发时把 /api 代理到本地后端 —— 后端主力是 Java（my-h5-server，8080 端口）；
      // Node 版（my-h5-api，3000 端口）冻结保留作对照。
      // rewrite 把 /api 前缀去掉再转发：后端不配 context-path，前缀是代理层的职责（工程手册第 1 章裁定）。
      // （技能点：跨域与代理——浏览器同源策略只限“浏览器直连”，
      //  开发服务器是 Node 进程转发请求，不受同源策略约束；生产环境要换 CORS 或同域部署）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 头像等静态资源：开发时同样代理到后端 8080。
      // 后端 WebMvcConfig 把本地上传目录映射为 /uploads/** 对外访问，
      // 前端写相对路径 /uploads/xxx.png，由这里转发到 8080 取文件。
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
