import { createApp } from 'vue'
import Cookies from 'js-cookie'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import '@/assets/styles/element-theme.scss'
import '@/assets/styles/index.scss'
import '@/assets/styles/ruoyi.scss'
import '@/assets/icons' // 构建 svg 雪碧图

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
import Editor from '@/components/Editor'
import TreePanel from '@/components/TreePanel'
import ExcelImportDialog from '@/components/ExcelImportDialog'

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

import './permission' // 路由守卫

app.mount('#app')
