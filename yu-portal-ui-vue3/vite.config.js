import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

// 对应旧 vue.config.js：端口 81、/dev-api 代理到后端 8080、@ 指向 src
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const baseUrl = 'http://localhost:8080'
  return {
    base: '/',
    resolve: {
      alias: { '@': path.resolve(fileURLToPath(new URL('.', import.meta.url)), 'src') },
      extensions: ['.mjs', '.js', '.json', '.vue']
    },
    plugins: [vue()],
    css: {
      preprocessorOptions: {
        // 采用 Dart Sass 现代编译 API，消除 legacy JS API 弃用告警
        scss: { api: 'modern-compiler' }
      }
    },
    server: {
      host: '0.0.0.0',
      port: 81,
      open: true,
      proxy: {
        [env.VITE_APP_BASE_API]: {
          target: baseUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(new RegExp('^' + env.VITE_APP_BASE_API), '')
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'static',
      sourcemap: false
    }
  }
})
