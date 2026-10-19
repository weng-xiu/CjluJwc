import axios from 'axios'
import { Message, MessageBox, Notification, Loading } from 'element-ui'
import { getToken, removeToken } from '@/utils/auth'

// E1 门户请求层能力对齐：与管理端 utils/request.js 对齐——GET 参数序列化、errorCode 映射、
// blob/arraybuffer 透传、防重复提交、通用 download 通道，统一错误提示口径。
// 差异保留：门户为极简运行时，不引入 plugins/cache 与 file-saver——防重复提交用原生 sessionStorage，
// download 用原生 <a> + ObjectURL 落盘；401 重新登录沿用门户自身 /login 路由与 removeToken。
// 保留 A7：路由切换统一取消在途请求的能力（cancelPendingRequests），与管理端口径互补。

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

// 通用错误码映射（自包含，避免额外依赖 utils/errorCode.js）
const errorCode = {
  401: '认证失败，无法访问系统资源',
  403: '当前操作没有权限',
  404: '访问资源不存在',
  default: '系统未知错误，请反馈给管理员'
}

let downloadLoadingInstance
let isRelogin = false

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

// get 请求参数序列化：数组/嵌套对象展开（与管理端口径一致，避免后端 @RequestParam 解析错位）
function tansParams(params) {
  let result = ''
  for (const propName of Object.keys(params)) {
    const value = params[propName]
    const part = encodeURIComponent(propName) + '='
    if (value !== null && value !== '' && typeof value !== 'undefined') {
      if (typeof value === 'object') {
        for (const key of Object.keys(value)) {
          if (value[key] !== null && value[key] !== '' && typeof value[key] !== 'undefined') {
            const params2 = propName + '[' + key + ']'
            const subPart = encodeURIComponent(params2) + '='
            result += subPart + encodeURIComponent(value[key]) + '&'
          }
        }
      } else {
        result += part + encodeURIComponent(value) + '&'
      }
    }
  }
  return result
}

// 防重复提交：同一 url+data 在 interval 内拦截（post/put），用原生 sessionStorage 记录上一请求
function isRepeatSubmit(config) {
  const interval = (config.headers || {}).interval || 1000
  const requestObj = {
    url: config.url,
    data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
    time: new Date().getTime()
  }
  const requestSize = Object.keys(JSON.stringify(requestObj)).length
  const limitSize = 5 * 1024 * 1024
  if (requestSize >= limitSize) {
    console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
    return false
  }
  let sessionObj
  try { sessionObj = JSON.parse(sessionStorage.getItem('sessionObj')) } catch (e) { sessionObj = null }
  if (!sessionObj) {
    sessionStorage.setItem('sessionObj', JSON.stringify(requestObj))
    return false
  }
  if (sessionObj.data === requestObj.data && requestObj.time - sessionObj.time < interval && sessionObj.url === requestObj.url) {
    const message = '数据正在处理，请勿重复提交'
    console.warn(`[${sessionObj.url}]: ` + message)
    return true
  }
  sessionStorage.setItem('sessionObj', JSON.stringify(requestObj))
  return false
}

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  const isRepeat = (config.headers || {}).repeatSubmit === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  // E1：GET 参数序列化
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  // E1：防重复提交
  if (!isRepeat && (config.method === 'post' || config.method === 'put')) {
    if (isRepeatSubmit(config)) {
      return Promise.reject(new Error('数据正在处理，请勿重复提交'))
    }
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

// blob 有效性校验：后端返回 JSON 错误对象时不是合法 blob
function blobValidate(data) {
  return data.type !== 'application/json'
}

service.interceptors.response.use(res => {
  // A7：请求完成，从在途登记集移除
  if (res.config && res.config.__abortController) {
    pendingControllers.delete(res.config.__abortController)
  }
  // E1：blob / arraybuffer 直接透传，交由 download 通道校验（否则二进制被当 JSON 解析取 code 出错）
  if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
    return res.data
  }
  const code = res.data.code || 200
  const msg = errorCode[code] || res.data.msg || errorCode['default']
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
  } else if (code === 601) {
    Message({ message: msg, type: 'warning' })
    return Promise.reject(new Error(msg))
  } else if (code !== 200) {
    Notification.error({ title: msg })
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
  } else if (message.includes('Request failed with status code')) {
    message = '系统接口' + message.slice(-3) + '异常'
  }
  Message({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

// E1：通用下载方法（门户导出能力补齐）：POST 表单 → blob，非 blob 则解析后端错误对象
export function download(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({ text: '正在下载数据，请稍候', background: 'rgba(0, 0, 0, 0.7)' })
  return service.post(url, params, {
    transformRequest: [(p) => tansParams(p)],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = data instanceof Blob ? data : new Blob([data])
      saveBlob(blob, filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      Message.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch(() => {
    console.error('下载文件出现错误，请联系管理员！')
    Message.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close()
  })
}

// 原生落盘（无 file-saver）：创建临时 <a download> 触发下载并回收 ObjectURL
function saveBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default service
