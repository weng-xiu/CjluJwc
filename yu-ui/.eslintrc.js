/**
 * D21 前端工程化收口：管理端（Vue2 主栈 yu-ui）ESLint 基线。
 *
 * 版本选择（与 yu-ui-vue3 同源）：ESLint 8.57 + eslint-plugin-vue 9.33
 * （plugin v9 非 vue3 预设即为 Vue2 规则集，Node >=16 均可运行）；
 * prettier 仅通过 eslint-config-prettier 关闭冲突规则，不对存量代码做格式化改写。
 *
 * 规则取向：只把「真 bug」列为 error（未定义变量、模板解析错误、重复键、v-for 缺 key 等），
 * 把「风格 / 历史包袱」降级为 warn（未使用变量、属性顺序、组件命名等），使门禁今天即可通过，
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
    // 本工程表单生成器 / 侧边栏菜单项等用 render 函数 + JSX（babel 保留 jsx 插件），
    // 必须让 espree 按 JSX 解析，否则会报 "Parsing error: Unexpected token <"
    ecmaFeatures: { jsx: true }
  },
  // 注：eslint-plugin-vue v9 的 eslintrc 预设中，essential / strongly-recommended / recommended
  // 默认面向 Vue2；vue3-* 才是 Vue3。故此处用 essential 即为 Vue2 基线。
  extends: ['eslint:recommended', 'plugin:vue/essential', 'prettier'],
  plugins: ['vue'],
  globals: {
    // RuoYi 全局挂载 / 构建期常量的只读占位
    require: 'readonly',
    process: 'readonly',
    __webpack_public_path__: 'writable'
  },
  ignorePatterns: [
    'dist/**',
    'node_modules/**',
    'public/**',
    'src/assets/**',
    '*.min.js'
  ],
  rules: {
    // —— 历史代码宽松项：warn，不阻断 ——
    'no-unused-vars': ['warn', { args: 'none', caughtErrors: 'none' }],
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'no-prototype-builtins': 'warn',
    // UI 文案里用全角空格（U+3000）作字段分隔符，是有意的排版选择
    'no-irregular-whitespace': 'warn',
    // 组件 name 与 HTML 保留标签同名（Data / Menu），仅影响页内 DOM 模板，本项目均为 SFC
    'vue/no-reserved-component-names': 'warn',
    // `_` 前缀实例属性（计时器/防抖句柄）仅作私有字段，不参与响应式，Vue2 不代理属预期行为
    'vue/no-reserved-keys': 'warn',
    // RemoteSelect 以 style prop + :style 绑定的方式受控宽度，上游用法
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
    },
    {
      // Crontab 从 RuoYi 原样搬运，computed 边读边写是上游设计，重写需独立需求
      files: ['src/components/Crontab/**/*.vue'],
      rules: { 'vue/no-side-effects-in-computed-properties': 'warn' }
    }
  ]
}
