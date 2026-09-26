import request from '@/utils/request'

// 字段映射说明
export function listReportFields(reportType) {
  return request({ url: '/system/statusReport/fields', method: 'get', params: { reportType } })
}

// 上报范围院系下拉
export function getDeptOptions() {
  return request({ url: '/system/statusReport/deptOptions', method: 'get' })
}

// 报盘数据预览（脱敏）
export function previewReportData(query) {
  return request({ url: '/system/statusReport/preview', method: 'get', params: query })
}

// 上报批次列表
export function listReportBatch(query) {
  return request({ url: '/system/statusReport/batchList', method: 'get', params: query })
}

// 一键生成上报批次
export function generateReport(data) {
  return request({ url: '/system/statusReport/generate', method: 'post', data })
}

// 标记批次已上报
export function submitReport(batchId) {
  return request({ url: '/system/statusReport/submit/' + batchId, method: 'put' })
}

// 作废批次
export function cancelReport(batchId) {
  return request({ url: '/system/statusReport/cancel/' + batchId, method: 'put' })
}

// 删除批次留痕
export function delReportBatch(batchId) {
  return request({ url: '/system/statusReport/batch/' + batchId, method: 'delete' })
}
