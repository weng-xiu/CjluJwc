import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'

// 主题（对应旧 element-variables.scss，见 element-theme.scss 顶部迁移说明）
import 'element-plus/dist/index.css'
import './assets/styles/element-theme.scss'
import './assets/styles/index.scss'

import App from './App.vue'
import store from './store'
import router from './router'
import Pagination from '@/components/Pagination/index.vue'

const app = createApp(App)

// 报告第3节 #1：el-icon-* 类名 → 图标组件。全局注册后模板可用 <el-icon><Xxx/></el-icon>
// 及 <component :is="'Calendar'"/>（菜单/快捷入口按配置组件名动态渲染）。
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// 报告第四节 #1：element-ui 全局尺寸 medium → element-plus default
// 中文 locale 保证分页/对话框等内置文案与 element-ui 时期一致。
app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.use(store)
app.use(router)

/**
 * 报告第3节 #6/#8：Vue.prototype.xxx = y → app.config.globalProperties.xxx = y
 * 门户此前未挂 $tab/$auth 等，这里补齐与 Iframe/Modal 插件等价的最小 $modal 门面，
 * 验证「业务代码里 this.$xxx 在 Options API 下仍可用」这一全站基础设施迁移口径。
 */
app.config.globalProperties.$modal = {
  msg(content) { ElMessage({ message: content, type: 'success' }) },
  msgError(content) { ElMessage({ message: content, type: 'error' }) },
  confirm(content) { return ElMessageBox.confirm(content, '系统提示', { type: 'warning' }) },
  notify(content) { ElNotification({ title: '提示', message: content, type: 'success' }) }
}

// 通用时间格式化（对应若依 utils/ruoyi 的 parseTime 挂载习惯）
app.config.globalProperties.parseTime = (time, pattern = '{y}-{m}-{d} {h}:{i}:{s}') => {
  const date = time instanceof Date ? time : new Date(time)
  const o = { y: date.getFullYear(), m: date.getMonth() + 1, d: date.getDate(), h: date.getHours(), i: date.getMinutes(), s: date.getSeconds() }
  return pattern.replace(/{([ymdhis])+}/g, (r, k) => String(o[k]).padStart(r.length > 3 ? 2 : 1, '0'))
}

/**
 * 报告第四节 #4：element-plus 默认不把 $message/$confirm 等挂到实例上，
 * 这里在 globalProperties 上保留 $ 别名，业务代码 this.$message.info/this.$confirm 零改写。
 */
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$notify = ElNotification
app.config.globalProperties.$msgbox = ElMessageBox
app.config.globalProperties.$alert = ElMessageBox.alert
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.config.globalProperties.$prompt = ElMessageBox.prompt

// 全局注册通用组件（Pagination 对应报告第七节 P2 通用组件搬迁）
app.component('Pagination', Pagination)

app.mount('#app')
