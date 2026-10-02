/**
 * V4.0 §7.3 前端工程化收口：管理端 ESLint 基线。
 *
 * 版本选择（受「依赖升级需验证兼容性」约束）：
 * 本地 Node 为 18.12，ESLint 9 要求 ^18.18、ESLint 10 要求 ^20，均不满足；
 * 故锁定 ESLint 8.57 + eslint-plugin-vue 9（均兼容 Node >=16.0），CI 用 Node 20 亦可运行。
 * 因本工程 package.json 为 "type": "module"，CommonJS 配置需使用 .cjs 后缀。
 *
 * 规则取向：只把「真 bug」列为 error（未定义变量、模板语法错误、重复键、v-for 缺 key 等），
 * 把「风格/历史包袱」降级为 warn（未使用变量、属性顺序、组件命名等），
 * 使门禁今天即可通过，同时保留回归拦截能力；后续按 warn 数量棘轮收紧。
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  extends: ['eslint:recommended', 'plugin:vue/vue3-essential', 'prettier'],
  plugins: ['vue'],
  globals: {
    // Vite 注入与构建期常量
    __APP_ENV__: 'readonly',
    __VUE_PROD_DEVTOOLS__: 'readonly',
    global: 'readonly',
    // 若依前端全局挂载（main.js app.config.globalProperties）
    require: 'readonly'
  },
  ignorePatterns: [
    'dist/**',
    'node_modules/**',
    'public/**',
    'src/assets/**',
    '*.min.js'
  ],
  rules: {
    // —— 历史代码宽松项：warn，不阻塞合并 ——
    'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off', // 富文本/公告渲染确需 v-html，内容由后端 XSS 过滤兜底
    // 若依惯用法：父页把 form/info 对象整体作 prop 传子组件，子组件 v-model 其内部字段。
    // 重构为 emit 需改动 30+ 个弹窗，回归风险大于收益，故降为 warn；新组件应用 emit + 局部状态。
    'vue/no-mutating-props': 'warn',
    // 组件 name（如 Data / Menu）与 tagsView 的 keep-alive include 匹配相关，
    // 改名会造成页面缓存失效，存量页保留，仅提醒新增页面避免 HTML 保留名。
    'vue/no-reserved-component-names': 'warn',

    // —— 硬约束：error ——
    'no-undef': 'error',
    'no-dupe-keys': 'error',
    'no-dupe-args': 'error',
    'no-duplicate-case': 'error',
    'no-unreachable': 'error',
    'no-irregular-whitespace': 'error',
    'vue/require-v-for-key': 'error',
    'vue/no-parsing-error': 'error',
    'vue/no-duplicate-attributes': 'error',
    'vue/no-use-v-if-with-v-for': 'error',
    'vue/valid-next-tick': 'error',
    // Vue3：$listeners / .sync / v-model 旧语法等在 Vue3 已失效，属真实缺陷
    'vue/no-deprecated-v-bind-sync': 'error',
    'vue/no-deprecated-v-on-native-modifier': 'error',
    'vue/no-deprecated-dollar-listeners-api': 'error',
    'vue/no-deprecated-events-api': 'error'
  },
  overrides: [
    {
      // 构建/配置脚本运行在 Node
      files: ['vite.config.js', '*.cjs', '*.config.js'],
      env: { node: true, browser: false }
    },
    {
      // Crontab 为从 RuoYi 原样搬迁的第三方表达式组件，其 computed 边读边写是上游设计；
      // 重写为 methods + 显式 data 会改变表达式预览行为，需独立需求推进，暂降为 warn。
      files: ['src/components/Crontab/**/*.vue'],
      rules: { 'vue/no-side-effects-in-computed-properties': 'warn' }
    }
  ]
}
