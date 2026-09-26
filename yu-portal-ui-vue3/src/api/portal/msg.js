import request from '@/utils/request'

// 我的消息列表
export function listMyMessages(query) {
  return request({ url: '/portal/msg/list', method: 'get', params: query })
}

// 标记单条已读
export function markMessageRead(messageId) {
  return request({ url: '/portal/msg/read/' + messageId, method: 'put' })
}

// 全部标记已读
export function markAllMessagesRead() {
  return request({ url: '/portal/msg/readAll', method: 'put' })
}

// 未读消息数
export function getUnreadCount() {
  return request({ url: '/portal/msg/unreadCount', method: 'get' })
}

// 我的待办列表
export function listMyTodos(query) {
  return request({ url: '/portal/msg/todoList', method: 'get', params: query })
}

// 办结待办
export function completeTodo(todoId) {
  return request({ url: '/portal/msg/todo/complete/' + todoId, method: 'post' })
}

// 待办数量
export function getPendingCount() {
  return request({ url: '/portal/msg/todo/pendingCount', method: 'get' })
}
