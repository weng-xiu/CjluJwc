import request from '@/utils/request'

// 查询当前用户消息列表
export function listMessage(query) {
  return request({ url: '/system/msgCenter/message/list', method: 'get', params: query })
}

// 标记消息已读
export function markMessageRead(messageId) {
  return request({ url: '/system/msgCenter/message/read/' + messageId, method: 'put' })
}

// 全部标记已读
export function markAllMessageRead() {
  return request({ url: '/system/msgCenter/message/readAll', method: 'put' })
}

// 未读消息数
export function getUnreadCount() {
  return request({ url: '/system/msgCenter/message/unreadCount', method: 'get' })
}

// 查询当前用户待办列表
export function listTodo(query) {
  return request({ url: '/system/msgCenter/todo/list', method: 'get', params: query })
}

// 完成待办
export function completeTodo(todoId) {
  return request({ url: '/system/msgCenter/todo/complete/' + todoId, method: 'post' })
}

// 待办数量
export function getPendingCount() {
  return request({ url: '/system/msgCenter/todo/pendingCount', method: 'get' })
}
