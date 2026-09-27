import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

// 对应旧 yu-ui/vue.config.js：dev 端口 82（80 为 Vue2 管理端、81 为门户 Vue3 试点，互不占用）、
// /dev-api 代理后端 8080、@ 指向 src。
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
        scss: { api: 'modern-compiler' }
      }
    },
    server: {
      host: '0.0.0.0',
      port: 82,
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
      sourcemap: false,
      chunkSizeWarningLimit: 2000
    }
  }
})
