import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

// 对应旧 vue.config.js：端口 81、/dev-api 代理到后端 8080、@ 指向 src
//
// V4.0 §7.3/U1 包体治理（与 yu-ui-vue3 同口径，见那边顶部的三点说明）：
// 门户无 echarts/富文本等重依赖，主要收益来自把 vue 运行时与 element-plus 拆开，
// 让业务改动机不影响框架 chunk 的缓存命中。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  // V4.0 §7.3/C3：代理目标不再写死本机地址，可用 .env.local 或环境变量覆盖
  const baseUrl = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const isProd = mode === 'production'
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
      port: Number(env.VITE_DEV_PORT || 81),
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
      chunkSizeWarningLimit: 1000,
      reportCompressedSize: false,
      minify: 'esbuild',
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            const p = id.replace(/\\/g, '/')
            if (/\/node_modules\/(vue|@vue\/[^/]+|vue-router|vuex)\//.test(p)) return 'vue-vendor'
            if (p.includes('/element-plus/') || p.includes('/@element-plus/')) return 'element-plus'
            if (p.includes('/axios/') || p.includes('/js-cookie/')) return 'http-utils'
            return 'vendor'
          }
        }
      }
    },
    esbuild: isProd
      ? { drop: ['debugger'], pure: ['console.log', 'console.debug', 'console.info'] }
      : {}
  }
})
