import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import { getToken, removeToken } from '@/utils/auth'

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
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
