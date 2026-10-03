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
      //
      // ⚠️ 【依赖健康度警告】（2026-10-03 补充，详见 docs/ADR-001-移动端适配方案.md）
      // 该插件是社区 fork，最后发版 2024-03，已两年多未更新；
      // 原版 postcss-px-to-viewport（evrone）更早停更且基于 PostCSS 7 API。
      // 为什么还留着：它只在构建期做「px → vw」的文本替换，产物是纯 CSS，
      // 不进入运行时、不污染业务代码，换掉它只需改这一段配置 —— 逃生成本极低。
      // 什么时候必须换：需要在平板/PC 上限制最大宽度时（纯 vw 会无限放大），
      // 换 postcss-mobile-forever（活跃维护，原生支持 maxWidth 与桌面居中）。
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
