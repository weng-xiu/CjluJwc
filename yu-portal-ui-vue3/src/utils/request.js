import axios from 'axios'
// Vue3 迁移：element-ui 的 Message/MessageBox 命名导入 → element-plus 的 ElMessage/ElMessageBox
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  // Vue3/Vite 迁移：process.env.VUE_APP_* → import.meta.env.VITE_APP_*
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 10000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  return config
}, error => {
  return Promise.reject(error)
})

let isRelogin = false

service.interceptors.response.use(res => {
  const code = res.data.code || 200
  const msg = res.data.msg || '系统错误'
  if (code === 401) {
    if (!isRelogin) {
      isRelogin = true
      ElMessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
        confirmButtonText: '重新登录', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        isRelogin = false
        removeToken()
        location.href = '/login'
      }).catch(() => { isRelogin = false })
    }
    return Promise.reject('无效的会话')
  } else if (code === 500) {
    ElMessage({ message: msg, type: 'error' })
    return Promise.reject(new Error(msg))
  } else if (code !== 200) {
    ElMessage({ message: msg, type: 'warning' })
    return Promise.reject(new Error(msg))
  } else {
    return res.data
  }
}, error => {
  let { message } = error
  if (message === 'Network Error') {
    message = '后端接口连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  }
  ElMessage({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

export default service
