import request from '@/utils/request'

// 查询公文列表
export function listDocument(query) {
  return request({ url: '/oa/document/list', method: 'get', params: query })
}

// 查询公文详细
export function getDocument(documentId) {
  return request({ url: '/oa/document/' + documentId, method: 'get' })
}

// 新增公文
export function addDocument(data) {
  return request({ url: '/oa/document', method: 'post', data: data })
}

// 修改公文
export function updateDocument(data) {
  return request({ url: '/oa/document', method: 'put', data: data })
}

// 删除公文
export function delDocument(documentId) {
  return request({ url: '/oa/document/' + documentId, method: 'delete' })
}

// 导出公文
export function exportDocument(query) {
  return request({ url: '/oa/document/export', method: 'post', params: query, responseType: 'blob' })
}

// 提交审批
export function submitDocument(documentId) {
  return request({ url: '/oa/document/submit/' + documentId, method: 'post' })
}

// 审批通过
export function approveDocument(data) {
  return request({ url: '/oa/document/approve', method: 'post', params: data })
}

// 审批驳回
export function rejectDocument(data) {
  return request({ url: '/oa/document/reject', method: 'post', params: data })
}

// 撤回公文
export function cancelDocument(documentId) {
  return request({ url: '/oa/document/cancel/' + documentId, method: 'post' })
}

// 待办公文
export function listTodoDocument(query) {
  return request({ url: '/oa/document/todo', method: 'get', params: query })
}

// 已办公文
export function listDoneDocument(query) {
  return request({ url: '/oa/document/done', method: 'get', params: query })
}
