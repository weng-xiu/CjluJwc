import axios from 'axios'
// Vue3 迁移：element-ui 的 Message/MessageBox 命名导入 → element-plus 的 ElMessage/ElMessageBox
import { ElMessage, ElMessageBox, ElNotification, ElLoading } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'

// E1 门户请求层能力对齐：与管理端 utils/request.js 对齐——GET 参数序列化、errorCode 映射、
// blob/arraybuffer 透传、防重复提交、通用 download 通道，统一错误提示口径。
// 差异保留：门户无 file-saver 依赖（离线不可安装），download 用原生 <a> + ObjectURL 落盘；
// 401 重新登录沿用门户自身 /login 路由与 removeToken，不依赖管理端 store 的 LogOut。

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

let downloadLoadingInstance

const service = axios.create({
  // Vue3/Vite 迁移：process.env.VUE_APP_* → import.meta.env.VITE_APP_*
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 10000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  const interval = (config.headers || {}).interval || 1000
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  // get 请求映射 params 参数（数组/嵌套对象展开，与管理端口径一致，避免后端 @RequestParam 解析错位）
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  // 防重复提交：同一 url+data 在 interval 内拦截（post/put）
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length
    const limitSize = 5 * 1024 * 1024
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url
      const s_data = sessionObj.data
      const s_time = sessionObj.time
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === sessionObj.url) {
        const message = '数据正在处理，请勿重复提交'
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
  return Promise.reject(error)
})

let isRelogin = false

service.interceptors.response.use(res => {
  // blob / arraybuffer 直接透传，交由 download 通道校验（否则二进制被当 JSON 解析取 code 出错）
  if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
    return res.data
  }
  const code = res.data.code || 200
  const msg = errorCode[code] || res.data.msg || errorCode['default']
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
  } else if (code === 601) {
    ElMessage({ message: msg, type: 'warning' })
    return Promise.reject(new Error(msg))
  } else if (code !== 200) {
    ElNotification.error({ title: msg })
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
  } else if (message.includes('Request failed with status code')) {
    message = '系统接口' + message.slice(-3) + '异常'
  }
  ElMessage({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

// 通用下载方法（门户导出能力补齐）：POST 表单 → blob，非 blob 则解析后端错误对象
export function download(url, params, filename, config) {
  downloadLoadingInstance = ElLoading.service({ text: '正在下载数据，请稍候', background: 'rgba(0, 0, 0, 0.7)' })
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
      ElMessage.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch(() => {
    console.error('下载文件出现错误，请联系管理员！')
    ElMessage.error('下载文件出现错误，请联系管理员！')
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
