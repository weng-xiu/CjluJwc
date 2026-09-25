import request from '@/utils/request'

// 查询打印凭证模板列表
export function listPrintTemplate(query) {
  return request({ url: '/system/printTemplate/list', method: 'get', params: query })
}

// 查询打印凭证模板详细
export function getPrintTemplate(templateId) {
  return request({ url: '/system/printTemplate/' + templateId, method: 'get' })
}

// 新增打印凭证模板
export function addPrintTemplate(data) {
  return request({ url: '/system/printTemplate', method: 'post', data: data })
}

// 修改打印凭证模板
export function updatePrintTemplate(data) {
  return request({ url: '/system/printTemplate', method: 'put', data: data })
}

// 删除打印凭证模板
export function delPrintTemplate(templateIds) {
  return request({ url: '/system/printTemplate/' + templateIds, method: 'delete' })
}

// 模板测试渲染（示例数据，返回 HTML）
export function previewPrintTemplate(templateId) {
  return request({ url: '/system/printTemplate/preview/' + templateId, method: 'get' })
}
