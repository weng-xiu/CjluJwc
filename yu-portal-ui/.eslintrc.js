/**
 * D21 前端工程化收口：门户端（Vue2 yu-portal-ui）ESLint 基线。
 *
 * 与管理端 yu-ui 完全同源：ESLint 8.57 + eslint-plugin-vue 9.33 + eslint-config-prettier；
 * 两端共用一套规则，避免同一份代码在两侧判定不一致。
 *
 * 规则取向：只把「真 bug」列为 error（未定义变量、模板解析错误、重复键、v-for 缺 key 等），
 * 把「风格 / 历史包袱」降级为 warn（未使用变量、props 直改、属性顺序等），使门禁今天即可通过，
 * 同时保留回拦能力；后续按 warn 数量棘轮收紧。
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    node: true,
    es2022: true
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: 'module',
    // 与管理端保持一致：render 函数内可能出现 JSX
    ecmaFeatures: { jsx: true }
  },
  // 注：eslint-plugin-vue v9 的 eslintrc 预设中，essential 等非 vue3 预设即为 Vue2 规则集。
  extends: ['eslint:recommended', 'plugin:vue/essential', 'prettier'],
  plugins: ['vue'],
  globals: {
    require: 'readonly',
    process: 'readonly',
    __webpack_public_path__: 'writable'
  },
  ignorePatterns: ['dist/**', 'node_modules/**', 'public/**', 'src/assets/**', '*.min.js'],
  rules: {
    // —— 历史代码宽松项：warn，不阻断 ——
    'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'no-prototype-builtins': 'warn',
    // UI 文案里用全角空格（U+3000）作字段分隔符，是有意的排版选择
    'no-irregular-whitespace': 'warn',
    // 组件 name 与 HTML 保留标签同名，仅影响页内 DOM 模板，本项目均为 SFC
    'vue/no-reserved-component-names': 'warn',
    // `_` 前缀实例属性仅作私有字段，不参与响应式，Vue2 不代理属预期行为
    'vue/no-reserved-keys': 'warn',
    'vue/no-reserved-props': 'warn',

    // —— 硬约束：error（真 bug 类） ——
    'no-undef': 'error',
    'no-dupe-keys': 'error',
    'no-dupe-args': 'error',
    'no-duplicate-case': 'error',
    'no-unreachable': 'error',
    'no-self-assign': 'error',
    'no-cond-assign': 'error',
    'vue/require-v-for-key': 'error',
    'vue/no-parsing-error': 'error',
    'vue/no-duplicate-attributes': 'error',
    'vue/no-use-v-if-with-v-for': 'error',
    'vue/valid-v-for': 'error',
    'vue/valid-v-model': 'error',
    'vue/no-side-effects-in-computed-properties': 'warn',
    'vue/no-mutating-props': 'warn',
    'vue/multi-word-component-names': 'off'
  },
  overrides: [
    {
      // 构建脚本运行在 Node
      files: ['build/**/*.js', 'vue.config.js', 'babel.config.js', '*.config.js'],
      env: { node: true, browser: false }
    }
  ]
}
