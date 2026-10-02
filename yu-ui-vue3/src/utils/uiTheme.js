/**
 * U1 主题色机构自定义工具（补充 utils/theme.js 的暗色切换能力）。
 *
 * - 机构主题色存储于后端 sys_config 参数 sys.ui.themeColor（十六进制），
 *   由具备 system:config:edit 权限的管理员在导航栏调色盘"同步到机构"写入；
 * - 前端按 Element Plus 混白/混黑规则派生 light-3/5/7/8/9 与 dark-2 色阶，
 *   以行内样式覆盖 :root 的 --el-color-primary-*（优先级高于样式表，运行时换肤）；
 *   同时写入 --dt-color-primary 设计令牌，供自有组件引用；
 * - 本地偏好存 localStorage['app-theme-color']，首屏立即应用避免闪色；
 *   登录进入系统后（permission.js GetInfo 成功时）拉取机构参数对齐覆盖。
 */
import { getConfigKey, addConfig, updateConfig, listConfig } from '@/api/system/config'
import { notifyThemeChanged } from '@/utils/theme'

/** 长江大学品牌默认主色（与 element-theme.scss / tokens.scss 内置值一致） */
export const DEFAULT_PRIMARY = '#007ab8'
/** 机构主题色的 sys_config 参数键 */
export const THEME_COLOR_CONFIG_KEY = 'sys.ui.themeColor'

const STORAGE_KEY = 'app-theme-color'
const HEX_RE = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/

function normalizeHex(color) {
  if (typeof color !== 'string') return null
  const c = color.trim()
  if (!HEX_RE.test(c)) return null
  // #abc → #aabbcc
  if (c.length === 4) {
    return '#' + c[1] + c[1] + c[2] + c[2] + c[3] + c[3]
  }
  return c.toLowerCase()
}

function hexToRgb(hex) {
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16)
  ]
}

/** Element Plus 官方派生规则：light-n 为混白（n/10 权重白），dark-2 为混黑（0.2 权重黑） */
function mix(color, weight, target) {
  const [r, g, b] = hexToRgb(color)
  const [tr, tg, tb] = target
  const p = weight
  const to = (a, t) => Math.round((t - a) * p) + a
  return '#' + [to(r, tr), to(g, tg), to(b, tb)]
    .map((v) => v.toString(16).padStart(2, '0'))
    .join('')
}

const WHITE = [255, 255, 255]
const BLACK = [0, 0, 0]
/** Element Plus 官方暗色分支的色阶混色基色（theme-chalk dark css-vars 用 #141414） */
const DARK_BASE = [20, 20, 20]

/** 当前是否处于暗色（以 html.dark 为单一事实源，与 utils/theme.js 的类切换保持同步） */
function isDarkActive() {
  return document.documentElement.classList.contains('dark')
}

/** 计算给定主色的全套 CSS 变量映射（明暗分支各按 EP 官方混色规则，避免暗色下行内浅色覆盖） */
export function derivePrimaryVars(hex, dark = isDarkActive()) {
  const lightTarget = dark ? DARK_BASE : WHITE
  return {
    '--el-color-primary': hex,
    '--el-color-primary-light-3': mix(hex, 0.3, lightTarget),
    '--el-color-primary-light-5': mix(hex, 0.5, lightTarget),
    '--el-color-primary-light-7': mix(hex, 0.7, lightTarget),
    '--el-color-primary-light-8': mix(hex, 0.8, lightTarget),
    '--el-color-primary-light-9': mix(hex, 0.9, lightTarget),
    '--el-color-primary-dark-2': mix(hex, 0.2, dark ? WHITE : BLACK),
    '--dt-color-primary': hex
  }
}

/** 本地已存主题色偏好（无则返回默认色） */
export function getStoredThemeColor() {
  return normalizeHex(localStorage.getItem(STORAGE_KEY)) || DEFAULT_PRIMARY
}

// 明暗切换（utils/theme.js 派发 app-theme-change）时按新分支重算行内色阶，
// 否则浅色混入的 --el-color-primary-light-* 会持续覆盖 EP 暗色变量（plain 按钮近白底）。
window.addEventListener('app-theme-change', () => {
  writePrimaryVars(getStoredThemeColor())
})

/**
 * 将色值套写入 html 行内样式（不触发主题事件，供 apply/重算复用）。
 */
function writePrimaryVars(color) {
  const hex = normalizeHex(color) || DEFAULT_PRIMARY
  const vars = derivePrimaryVars(hex)
  const root = document.documentElement
  Object.entries(vars).forEach(([k, v]) => root.style.setProperty(k, v))
  return hex
}

/**
 * 应用主题色到全站（写入 html 行内样式覆盖 CSS 变量）。
 * persist=true 时同时记入本地偏好；color 非法则回落默认色。返回实际生效色值。
 */
export function applyThemeColor(color, persist = true) {
  const hex = writePrimaryVars(color)
  if (persist) {
    localStorage.setItem(STORAGE_KEY, hex)
  }
  notifyThemeChanged() // 图表系列色板随主题色联动（U4）
  return hex
}

/**
 * 登录后与机构配置对齐：读取 sys_config 的 sys.ui.themeColor，
 * 有合法配置值且与当前生效色不同时覆盖应用。静默失败（保持本地/默认色）。
 */
export function syncThemeColorFromConfig() {
  return getConfigKey(THEME_COLOR_CONFIG_KEY)
    .then((res) => {
      const hex = normalizeHex(res.msg)
      if (hex && hex !== getStoredThemeColor()) {
        applyThemeColor(hex)
      }
    })
    .catch(() => {})
}

/**
 * 管理员将主题色同步到机构（写 sys_config）。
 * 先按 configKey 查列表取 configId，再 updateConfig；无记录则 addConfig。
 * 权限由后端 @PreAuthorize('system:config:edit') 兜底。
 */
export function saveThemeColorToConfig(color) {
  const hex = normalizeHex(color)
  if (!hex) return Promise.reject(new Error('非法的颜色值'))
  return listConfig({ configKey: THEME_COLOR_CONFIG_KEY, pageNum: 1, pageSize: 10 }).then((res) => {
    const row = (res.rows || []).find((r) => r.configKey === THEME_COLOR_CONFIG_KEY)
    if (row) {
      return updateConfig({ ...row, configValue: hex })
    }
    return addConfig({
      configName: '界面-机构主题色',
      configKey: THEME_COLOR_CONFIG_KEY,
      configValue: hex,
      configType: 'Y',
      remark: '管理端全站主题色（十六进制），前端自动派生 light/dark 色阶'
    })
  })
}
