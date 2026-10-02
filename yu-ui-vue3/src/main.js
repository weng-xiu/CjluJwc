import { createApp, defineAsyncComponent } from 'vue'
import Cookies from 'js-cookie'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// U1 暗色模式：Element Plus 官方暗色变量（html.dark 作用域），配合 utils/theme.js 挂摘 dark 类
import 'element-plus/theme-chalk/dark/css-vars.css'

import '@/assets/styles/element-theme.scss'
import '@/assets/styles/tokens.scss' // U1 设计令牌（--dt-*，含暗色重映射）
import '@/assets/styles/responsive.scss' // U2 响应式三档断点（≥1920/1367–1919/≤1366）
import '@/assets/styles/index.scss'
import '@/assets/styles/ruoyi.scss'
import '@/assets/icons' // 构建 svg 雪碧图
import { initDarkMode } from '@/utils/theme'
import { applyThemeColor, getStoredThemeColor } from '@/utils/uiTheme'
import responsive from '@/utils/responsive' // U2 响应式屏幕（$screen 断点）

import App from './App.vue'
import store from './store'
import router from './router'
import directive from './directive'
import plugins from './plugins'
import DictData from '@/utils/dict'
import { download } from '@/utils/request'
import { getDicts } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from '@/utils/ruoyi'

import SvgIcon from '@/components/SvgIcon'
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'
import DictTag from '@/components/DictTag'
import ParentView from '@/components/ParentView'
import TreePanel from '@/components/TreePanel'
import ExcelImportDialog from '@/components/ExcelImportDialog'
// U3 统一状态反馈组件
import AppSkeleton from '@/components/AppSkeleton'
import AppEmpty from '@/components/AppEmpty'
import AppErrorState from '@/components/AppErrorState'
import AppBatchProgress from '@/components/AppBatchProgress'
import ImagePreview from '@/components/ImagePreview'

/**
 * V4.0 §7.3/U1 包体治理：echarts（BaseChart）与富文本 quill（Editor）是两个重依赖，
 * 此前作为普通全局组件被 main.js 静态引入，直接进了首屏 entry。
 * 改成 defineAsyncComponent 后，模板里 <BaseChart/> / <Editor/> 写法不变，
 * 但代码被 Vite 切到独立 chunk，仅在该组件真正渲染时才拉取。
 */
const BaseChart = defineAsyncComponent(() => import('@/components/BaseChart')) // U4 echarts 统一封装
const Editor = defineAsyncComponent(() => import('@/components/Editor')) // 富文本编辑器
const ImageUpload = defineAsyncComponent(() => import('@/components/ImageUpload')) // 图片上传（拖拽排序依赖 sortablejs，同样切出入口）

const app = createApp(App)

// 全局注册 element-plus 图标组件（<el-icon><Xxx/></el-icon> / <component :is="'Search'"/>）
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// element-ui 全局尺寸 medium → element-plus default；中文 locale 保持内置文案一致
app.use(ElementPlus, { locale: zhCn, size: Cookies.get('size') || 'default' })
app.use(store)
app.use(router)
app.use(directive)
app.use(plugins)
app.use(responsive) // U2 挂载 $screen（isNarrow/isWide/isMobile）
DictData.install(app)

// 全局方法挂载（对应 Vue.prototype.xxx）
app.config.globalProperties.getDicts = getDicts
app.config.globalProperties.getConfigKey = getConfigKey
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.download = download
app.config.globalProperties.handleTree = handleTree

// 全局组件挂载
app.component('DictTag', DictTag)
app.component('Pagination', Pagination)
app.component('RightToolbar', RightToolbar)
app.component('SvgIcon', SvgIcon)
app.component('ParentView', ParentView)
app.component('Editor', Editor)
app.component('TreePanel', TreePanel)
app.component('ExcelImportDialog', ExcelImportDialog)
// U3 统一状态反馈组件（全局可用，无需各页重复引入）
app.component('AppSkeleton', AppSkeleton)
app.component('AppEmpty', AppEmpty)
app.component('AppErrorState', AppErrorState)
app.component('AppBatchProgress', AppBatchProgress)
// U4 echarts 统一封装组件（主题/resize/空态自适应，全局可用）
app.component('BaseChart', BaseChart)
app.component('ImagePreview', ImagePreview) // 门户 CMS（轮播/文章）缩略图回显
app.component('ImageUpload', ImageUpload) // 门户 CMS 图片上传

import './permission' // 路由守卫

initDarkMode() // U1 按已存偏好初始化暗色，挂载前执行避免首屏闪白
applyThemeColor(getStoredThemeColor(), false) // U1 主题色：先应用本地偏好（机构值登录后对齐）
app.mount('#app')
