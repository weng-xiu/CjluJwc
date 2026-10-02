/**
 * V4.0 §7.3 前端工程化收口：门户端 ESLint 基线（与 yu-ui-vue3 同口径，便于统一治理）。
 *
 * 版本选择：本地 Node 18.12 不满足 ESLint 9（^18.18）/10（^20）的 engines 要求，
 * 故锁定 ESLint 8.57 + eslint-plugin-vue 9；本工程 package.json 为 "type": "module"，
 * CommonJS 配置需 .cjs 后缀。
 *
 * 规则取向：error 只留「真 bug」，历史风格项降为 warn，保证门禁当前可通过、后续棘轮收紧。
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
    __APP_ENV__: 'readonly',
    __VUE_PROD_DEVTOOLS__: 'readonly',
    global: 'readonly'
  },
  ignorePatterns: ['dist/**', 'node_modules/**', 'public/**', 'src/assets/**', '*.min.js'],
  rules: {
    'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off', // 门户公告/新闻详情为富文本渲染，内容由后端 XSS 过滤兜底

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
    'vue/no-deprecated-v-bind-sync': 'error',
    'vue/no-deprecated-v-on-native-modifier': 'error',
    'vue/no-deprecated-dollar-listeners-api': 'error',
    'vue/no-deprecated-events-api': 'error'
  },
  overrides: [
    {
      files: ['vite.config.js', '*.cjs', '*.config.js'],
      env: { node: true, browser: false }
    }
  ]
}
