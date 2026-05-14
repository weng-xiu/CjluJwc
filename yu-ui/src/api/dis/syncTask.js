import request from '@/utils/request'

// 查询数据同步任务列表
export function listSyncTask(query) {
  return request({ url: '/dis/task/list', method: 'get', params: query })
}

// 查询数据同步任务详细
export function getSyncTask(taskId) {
  return request({ url: '/dis/task/' + taskId, method: 'get' })
}

// 新增数据同步任务
export function addSyncTask(data) {
  return request({ url: '/dis/task', method: 'post', data: data })
}

// 修改数据同步任务
export function updateSyncTask(data) {
  return request({ url: '/dis/task', method: 'put', data: data })
}

// 删除数据同步任务
export function delSyncTask(taskId) {
  return request({ url: '/dis/task/' + taskId, method: 'delete' })
}

// 导出数据同步任务
export function exportSyncTask(query) {
  return request({ url: '/dis/task/export', method: 'post', params: query })
}
