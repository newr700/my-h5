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
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
