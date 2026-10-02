import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

// 对应旧 yu-ui/vue.config.js：dev 端口 82（80 为 Vue2 管理端、81 为门户 Vue3 试点，互不占用）、
// /dev-api 代理后端 8080、@ 指向 src。
//
// V4.0 §7.3/U1 包体治理：
//   1) manualChunks 把 element-plus / echarts / 编辑器等大依赖拆成可长期缓存的独立 chunk，
//      避免它们和业务代码混在同一个 entry 里被反复失效；
//   2) 业务侧重活（echarts 封装 BaseChart、富文本 Editor）改由 main.js 的
//      defineAsyncComponent 延迟加载，不在首屏 entry 内；
//   3) 传输层压缩交给部署侧 nginx（gzip / brotli）与 HTTP/2，不在构建产物里再存一份 .gz，
//      以免 .gz 与源文件不同步（本仓库无 lock 校验能力，双份产物反而更容易出错）。
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
        scss: { api: 'modern-compiler' }
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_DEV_PORT || 82),
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
      // 拆分后单个 chunk 的目标上限；element-plus 整体仍在 1MB 量级，留有余量避免噪音告警
      chunkSizeWarningLimit: 1000,
      // 产物体积报告需要 gzip 全量文件，CI 上耗时且不影响结果，关掉
      reportCompressedSize: false,
      // 生产构建剔除调试语句（保留 console.warn/error 以便线上门禁日志可查）
      minify: 'esbuild',
      rollupOptions: {
        output: {
          // 显式分包：命名稳定，便于长期缓存与 CDN 命中
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            const p = id.replace(/\\/g, '/')
            // vue 运行时全家桶必须独立且先于其他 chunk 求值，避免循环初始化问题
            if (/\/node_modules\/(vue|@vue\/[^/]+|vue-router|vuex)\//.test(p)) return 'vue-vendor'
            if (p.includes('/element-plus/') || p.includes('/@element-plus/')) return 'element-plus'
            if (p.includes('/echarts/') || p.includes('/zrender/')) return 'echarts'
            if (p.includes('/quill/') || p.includes('/highlight.js/')) return 'editor'
            if (p.includes('/vue-cropper/') || p.includes('/sortablejs/')) return 'widgets'
            if (p.includes('/axios/') || p.includes('/js-cookie/') || p.includes('/jsencrypt/') ||
                p.includes('/nprogress/') || p.includes('/file-saver/')) return 'http-utils'
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
