import request from '@/utils/request'

// 查询数据交换日志列表
export function listExchangeLog(query) {
  return request({ url: '/dis/exchange/list', method: 'get', params: query })
}

// 查询数据交换日志详细
export function getExchangeLog(logId) {
  return request({ url: '/dis/exchange/' + logId, method: 'get' })
}

// 删除数据交换日志
export function delExchangeLog(logId) {
  return request({ url: '/dis/exchange/' + logId, method: 'delete' })
}

// 清空数据交换日志
export function cleanExchangeLog() {
  return request({ url: '/dis/exchange/clean', method: 'delete' })
}

// 导出数据交换日志
export function exportExchangeLog(query) {
  return request({ url: '/dis/exchange/export', method: 'post', params: query })
}
