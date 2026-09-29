/**
 * U4 图表主题联动（首批：全局适配层，业务页零改动）。
 *
 * 现状：6 个统计页（resourceStat / monitor.cache / aiChatStat / subjectStat /
 * evaluationStat / gradeStatistics）均 `import * as echarts` 后直接 echarts.init(el)，
 * 暗色切换后坐标轴/文本仍是亮色默认主题，看不清且风格脱节。
 *
 * 方案：在应用入口对 echarts.init 打一层全局补丁——
 * 1) init 登记实例与业务页传入的原始 option（不篡改 option，仅记录）；
 * 2) 每次 setOption 实际渲染 = 原始 option 与「当前主题覆盖层」深合并，
 *    覆盖层文本/轴线/分隔线颜色直接引用运行时 --dt-* 设计令牌（U1），
 *    亮色模式下系列色板跟随机构主题色（--el-color-primary）派生；
 * 3) 监听 'app-theme-change'（暗色切换 / 主题色变更时由 utils/theme.js、utils/uiTheme.js
 *    派发），对全部登记实例以 notMerge 重放原始 option + 新主题覆盖层。
 *
 * 业务页保存的 chart 引用与 option 均不被修改，实例不重建，事件/resize 不受影响。
 * 后续如需 BaseChart 统一封装组件，可在此层之上渐进沉淀。
 */
import * as echarts from 'echarts'
import { isDarkEnabled } from '@/utils/theme'

/** 实例登记表：dom -> { instance, rawOption } */
const registry = new WeakMap()

function cssVar(name, fallback) {
  const v = (getComputedStyle(document.documentElement).getPropertyValue(name) || '').trim()
  return v || fallback
}

/** 与 Element Plus 混白规则一致的主色衍生（用于系列色板） */
function mixWhite(hex, weight) {
  const n = parseInt(hex.slice(1), 16)
  const r = (n >> 16) & 0xff
  const g = (n >> 8) & 0xff
  const b = n & 0xff
  const to = (a) => Math.round((255 - a) * weight) + a
  return '#' + [to(r), to(g), to(b)].map((v) => v.toString(16).padStart(2, '0')).join('')
}

function toHex6(hex) {
  if (hex.length === 4) {
    return '#' + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3]
  }
  return hex
}

/** 亮色模式下的系列色板：机构主题色领头 + 固定辅助色（与 Element 语义色一致） */
function lightPalette() {
  const primary = toHex6(cssVar('--el-color-primary', '#007ab8'))
  return [primary, '#67c23a', '#e6a23c', '#f56c6c', '#909399', mixWhite(primary, 0.5), mixWhite(primary, 0.75), '#9a6e4f']
}

/** 当前主题覆盖层：文本/轴线/分隔线/色板统一跟随 --dt-* 与机构主题色令牌（亮暗同源，不依赖 echarts 内置主题） */
function themeOverride() {
  const dark = isDarkEnabled()
  const text = cssVar('--dt-text-secondary', dark ? '#a3a6ad' : '#909399')
  const textStrong = cssVar('--dt-text-primary', dark ? '#e5eaf3' : '#303133')
  const axisLine = cssVar('--dt-border-color', dark ? '#414243' : '#dcdfe6')
  const splitLine = cssVar('--dt-border-color-light', dark ? '#363637' : '#e4e7ed')
  return {
    color: lightPalette(),
    textStyle: { color: text },
    title: { textStyle: { color: textStrong } },
    legend: { textStyle: { color: text } },
    categoryAxis: { axisLine: { lineStyle: { color: axisLine } }, axisLabel: { color: text }, splitLine: { lineStyle: { color: splitLine } } },
    valueAxis: { axisLine: { lineStyle: { color: axisLine } }, axisLabel: { color: text }, splitLine: { lineStyle: { color: splitLine } } },
    logAxis: { axisLabel: { color: text }, splitLine: { lineStyle: { color: splitLine } } },
    timeAxis: { axisLabel: { color: text }, splitLine: { lineStyle: { color: splitLine } } }
  }
}

/** 原始 option 与主题覆盖层合并（覆盖层不越权：业务页显式给出的 color 保留） */
function compose(rawOption) {
  const override = themeOverride()
  const merged = { ...(rawOption || {}) }
  Object.entries(override).forEach(([k, v]) => {
    if (v === undefined) return
    if (k === 'color' && merged.color) return // 业务页自定义色板优先
    merged[k] = merged[k] ? { ...v, ...merged[k] } : v
  })
  return merged
}

const originalInit = echarts.init.bind(echarts)

/** 补丁安装标记：setupChartTheme 调用一次（ESM 命名空间的 init 为只读，不能模块加载期赋值） */
function patchInit() {
  if (echarts.init !== originalInit) return
  try {
    echarts.init = function (dom, theme, opts) {
      const instance = originalInit(dom, isDarkEnabled() ? 'dark' : theme, opts)
      if (dom) {
        const entry = registry.get(dom) || {}
        entry.instance = instance
        registry.set(dom, entry)
      }
      const originalSetOption = instance.setOption.bind(instance)
      instance.setOption = function (option, ...args) {
        const entry = registry.get(dom)
        if (entry) entry.rawOption = option
        return originalSetOption(compose(option), ...args)
      }
      return instance
    }
  } catch (e) {
    // 命名空间只读无法打补丁时降级为不联动，不影响图表基础渲染
    console.warn('[chartTheme] echarts.init patch skipped', e)
  }
}

/** 主题（暗色/机构主色）变化：登记实例重放原始 option + 新覆盖层（notMerge 清掉旧主题残留） */
export function refreshChartsTheme() {
  document.querySelectorAll('div[_echarts_instance_]').forEach((dom) => {
    const entry = registry.get(dom)
    if (!entry || !entry.instance || !entry.rawOption) return
    // 绕过 compose 包装，直接以重放语义 notMerge 渲染
    entry.instance.setOption(entry.rawOption, true)
  })
}

let listening = false

/** 在应用入口安装图表主题联动（幂等）。 */
export function setupChartTheme() {
  if (listening) return
  listening = true
  patchInit()
  window.addEventListener('app-theme-change', refreshChartsTheme)
}
