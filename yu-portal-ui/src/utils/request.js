import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import { getToken, removeToken } from '@/utils/auth'

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

// A7：在途请求的 AbortController 登记集，路由切换时统一取消，防止上一页的迟到响应覆盖新页状态
const pendingControllers = new Set()

/**
 * A7：取消所有在途且标记为“可路由切换取消”的请求。通常在路由 beforeEach 中调用。
 */
export function cancelPendingRequests() {
  pendingControllers.forEach(controller => {
    try { controller.abort() } catch (e) { /* ignore */ }
  })
  pendingControllers.clear()
}

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  // A7：为可路由切换取消的请求登记 AbortController（默认开启，可传 cancelOnRouteChange=false 关闭）
  if (config.cancelOnRouteChange !== false && typeof AbortController !== 'undefined') {
    const controller = new AbortController()
    config.signal = controller.signal
    config.__abortController = controller
    pendingControllers.add(controller)
  }
  return config
}, error => {
  return Promise.reject(error)
})

let isRelogin = false

service.interceptors.response.use(res => {
  // A7：请求完成，从在途登记集移除
  if (res.config && res.config.__abortController) {
    pendingControllers.delete(res.config.__abortController)
  }
  const code = res.data.code || 200
  const msg = res.data.msg || '系统错误'
  if (code === 401) {
    if (!isRelogin) {
      isRelogin = true
      MessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
        confirmButtonText: '重新登录', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        isRelogin = false
        removeToken()
        location.href = '/login'
      }).catch(() => { isRelogin = false })
    }
    return Promise.reject('无效的会话')
  } else if (code === 500) {
    Message({ message: msg, type: 'error' })
    return Promise.reject(new Error(msg))
  } else if (code !== 200) {
    Message({ message: msg, type: 'warning' })
    return Promise.reject(new Error(msg))
  } else {
    return res.data
  }
}, error => {
  // A7：主动取消（路由切换）属预期行为，静默丢弃，不弹错误提示
  if (error.config && error.config.__abortController) {
    pendingControllers.delete(error.config.__abortController)
  }
  if (axios.isCancel && axios.isCancel(error)) {
    return Promise.reject(error)
  }
  let { message } = error
  if (message === 'Network Error') {
    message = '后端接口连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  }
  Message({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

export default service
