/**
 * E6 轻量 i18n：零依赖自建（不引 vue-i18n）。
 *
 * 为什么不引 vue-i18n：
 *   四个前端工程此前都没有该依赖、也没有任何 `$t(` 调用，一次性引入需要 npm install，
 *   在离线/锁版本环境下是纯风险；而本项目"国际化"的真实缺口不是框架能力，而是
 *   **文案散落在 400+ 个页面里，没有统一的取词入口**。先把入口立起来（$t + 字典 + 回退），
 *   页面才可能逐页采用；反过来先装框架却没有人调用，等于多了个空依赖。
 *
 * 契约（很重要，决定了能否安全地逐页采用）：
 *   1. `t(key, fallback)` —— 字典里有就用字典，没有就用 fallback，再没有就返回 key。
 *      所以采用方式是「把中文原文当 fallback 搬进 $t」，任何时刻渲染结果都与改造前一致，
 *      字典缺词不会让界面出现 `xxx.yyy` 这种技术串；
 *   2. 字典只登记**已经采用**的文案，不做全量抽取：字典里出现但代码没引用的键是死词，
 *      和硬编码一样要维护成本；
 *   3. 默认 zh-CN，`setLocale()` 记忆到 localStorage 并同步 <html lang>。
 *
 * 边界（明确不做，避免误以为已国际化）：
 *   - Element Plus / Element UI 组件库自身的语言仍固定为中文（Vue3 栈是 main.js 里
 *     静态 `app.use(ElementPlus, { locale: zhCn })`）。切换组件库语言需要动态 import
 *     语言包并处理异步首屏闪烁，等真有第二语言需求再做；
 *   - 后端返回的提示文案、字典标签、报表表头不在本模块范围内。
 */

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US']
export const DEFAULT_LOCALE = 'zh-CN'
const STORAGE_KEY = 'app-locale'

const MESSAGES = {
  'zh-CN': {
    'common.confirm': '确定',
    'common.cancel': '取消',
    'common.search': '搜索',
    'common.reset': '重置',
    'common.add': '新增',
    'common.edit': '修改',
    'common.remove': '删除',
    'common.export': '导出',
    'common.import': '导入',
    'common.close': '关闭',
    'common.save': '保存',
    'common.actions': '操作',
    'common.more': '更多',
    'message.operateSuccess': '操作成功',
    'message.operateFailed': '操作失败',
    'message.pleaseSelect': '请选择',
    'message.empty': '暂无数据',
    'dialog.dirty': '表单存在未保存的修改，确定要关闭吗？'
  },
  'en-US': {
    'common.confirm': 'OK',
    'common.cancel': 'Cancel',
    'common.search': 'Search',
    'common.reset': 'Reset',
    'common.add': 'Add',
    'common.edit': 'Edit',
    'common.remove': 'Delete',
    'common.export': 'Export',
    'common.import': 'Import',
    'common.close': 'Close',
    'common.save': 'Save',
    'common.actions': 'Actions',
    'common.more': 'More',
    'message.operateSuccess': 'Succeeded',
    'message.operateFailed': 'Failed',
    'message.pleaseSelect': 'Please select',
    'message.empty': 'No data',
    'dialog.dirty': 'This form has unsaved changes. Close it anyway?'
  }
}

function normalize(locale) {
  if (!locale) return null
  // 容忍 zh / zh-cn / zh_CN 之类的写法
  const hit = SUPPORTED_LOCALES.find((l) => l.toLowerCase() === String(locale).replace('_', '-').toLowerCase())
  return hit || null
}

function readStoredLocale() {
  try {
    return normalize(window.localStorage.getItem(STORAGE_KEY))
  } catch (e) {
    // 隐私模式下 localStorage 可能抛错：退回默认语言，不影响渲染
    return null
  }
}

let currentLocale = readStoredLocale() || DEFAULT_LOCALE

/** 当前语言 */
export function getLocale() {
  return currentLocale
}

/**
 * 切换语言：记忆到 localStorage 并同步 <html lang>（无障碍读屏与字体回退都依赖它）。
 * 返回实际生效的语言（非法入参不改变现状，返回当前语言而不是抛错）。
 */
export function setLocale(locale) {
  const hit = normalize(locale)
  if (!hit) return currentLocale
  currentLocale = hit
  try {
    window.localStorage.setItem(STORAGE_KEY, hit)
  } catch (e) {
    // 存不下就只对本次会话生效
  }
  if (typeof document !== 'undefined' && document.documentElement) {
    document.documentElement.setAttribute('lang', hit)
  }
  return currentLocale
}

/**
 * 取词：字典 → fallback → key。
 * 之所以把 fallback 放中间，是为了「中文原文即兜底」的渐进采用方式：
 * 改造后的渲染结果与硬编码完全一致，字典缺词也不会把技术键暴露给用户。
 */
export function t(key, fallback) {
  if (!key) return fallback || ''
  const table = MESSAGES[currentLocale] || MESSAGES[DEFAULT_LOCALE]
  const value = table[key]
  if (value !== undefined) return value
  if (fallback !== undefined && fallback !== null) return fallback
  // 当前语言缺词但默认语言有，则用默认语言兜底（例：en-US 尚未翻译的新词）
  const fallbackTable = MESSAGES[DEFAULT_LOCALE]
  if (fallbackTable && fallbackTable[key] !== undefined) return fallbackTable[key]
  return key
}

/** 启动时把 <html lang> 对齐到实际语言（index.html 里是静态值，可能与已记忆的语言不一致） */
export function initI18n() {
  if (typeof document !== 'undefined' && document.documentElement) {
    document.documentElement.setAttribute('lang', currentLocale)
  }
  return currentLocale
}

export default { t, getLocale, setLocale, initI18n, SUPPORTED_LOCALES, DEFAULT_LOCALE }
