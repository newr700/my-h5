/**
 * Vite 构建配置 —— 本文件是「工程化」技能点的聚集地。
 *
 * ── 面试技能点（详见 docs/面试技能树.md T3/T4/T5）────────
 * 路径别名（resolve.alias）/ 开发代理（server.proxy，解决跨域）/
 * PostCSS 移动适配（pxToViewport，vw 方案）
 * 考点速记：为什么 Vite 快？（esbuild 预构建 + 按需编译，不用先打包整个项目）
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import pxToViewport from 'postcss-px-to-viewport-8-plugin'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    postcss: {
      plugins: [
        // 按 375 设计稿直接写 px，构建时自动转 vw，适配所有手机
        pxToViewport({
          unitToConvert: 'px',
          viewportWidth: 375,
          unitPrecision: 5,
          propList: ['*'],
          viewportUnit: 'vw',
          fontViewportUnit: 'vw',
          selectorBlackList: ['.ignore-vw'],
          minPixelValue: 1,
          mediaQuery: false,
          exclude: [/node_modules/]
        })
      ]
    }
  },
  server: {
    proxy: {
      // 开发时把 /api 代理到本地后端，后端起在 3000 端口
      // （技能点：跨域与代理——浏览器同源策略只限“浏览器直连”，
      //  开发服务器是 Node 进程转发请求，不受同源策略约束；生产环境要换 CORS 或同域部署）
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
