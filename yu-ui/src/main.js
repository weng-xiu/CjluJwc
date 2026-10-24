import Vue from 'vue'

import Cookies from 'js-cookie'

import Element from 'element-ui'
import './assets/styles/element-variables.scss'
import '@/assets/styles/tokens.scss' // V2/U1 设计令牌（--dt-* + --el-* 兼容），须在使用前定义

import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
import '@/assets/styles/a11y.scss' // U5 无障碍基线（focus-visible/减少动画/skip-link，置于旧样式后覆盖 outline:none）
import '@/assets/styles/element-dark.scss' // V2 element-ui 暗色覆盖（须在 element-variables 编译出的全量 CSS 之后引入方能覆盖）
import { initDarkMode } from '@/utils/theme' // V2 暗色模式：按已存偏好初始化 html.dark，避免首屏闪白
initDarkMode()
import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

import './assets/icons' // icon
import './permission' // permission control
import { getDicts } from "@/api/system/dict/data"
import { getConfigKey } from "@/api/system/config"
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from "@/utils/ruoyi"
// 分页组件
import Pagination from "@/components/Pagination"
// 自定义表格工具组件
import RightToolbar from "@/components/RightToolbar"
// 富文本组件
import Editor from "@/components/Editor"
// 文件上传组件
import FileUpload from "@/components/FileUpload"
// 图片上传组件
import ImageUpload from "@/components/ImageUpload"
// 图片预览组件
import ImagePreview from "@/components/ImagePreview"
// 字典标签组件
import DictTag from '@/components/DictTag'
// 字典数据组件
import DictData from '@/components/DictData'
// U3/E3/E4 统一状态反馈组件（骨架屏/空态/错误态/批量进度，Vue2 主栈基建，自建替代 element-plus 专有组件）
import AppSkeleton from '@/components/AppSkeleton'
import AppEmpty from '@/components/AppEmpty'
import AppErrorState from '@/components/AppErrorState'
import AppBatchProgress from '@/components/AppBatchProgress'

// 全局方法挂载
Vue.prototype.getDicts = getDicts
Vue.prototype.getConfigKey = getConfigKey
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.addDateRange = addDateRange
Vue.prototype.selectDictLabel = selectDictLabel
Vue.prototype.selectDictLabels = selectDictLabels
Vue.prototype.download = download
Vue.prototype.handleTree = handleTree

// 全局组件挂载
Vue.component('DictTag', DictTag)
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)
Vue.component('Editor', Editor)
Vue.component('FileUpload', FileUpload)
Vue.component('ImageUpload', ImageUpload)
Vue.component('ImagePreview', ImagePreview)
// U3 统一状态反馈组件（全局可用，无需各页重复引入）
Vue.component('AppSkeleton', AppSkeleton)
Vue.component('AppEmpty', AppEmpty)
Vue.component('AppErrorState', AppErrorState)
Vue.component('AppBatchProgress', AppBatchProgress)

Vue.use(directive)
Vue.use(plugins)
DictData.install()

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
  size: Cookies.get('size') || 'medium' // set element-ui default size
})

Vue.config.productionTip = false

// A7：全局错误兜底，防止组件运行时异常直接白屏；记录日志并做非阻断式提示
Vue.config.errorHandler = function (err, vm, info) {
  console.error('[全局异常]', info, err)
  if (vm && vm.$modal && typeof vm.$modal.msgError === 'function') {
    vm.$modal.msgError('页面出现异常，请刷新重试')
  }
}

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
