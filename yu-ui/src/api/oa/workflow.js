import request from '@/utils/request'

// 部署流程文件
export function deployProcess(data) {
  return request({ url: '/oa/workflow/deploy', method: 'post', data: data })
}

// 查询流程定义列表
export function listDefinition(query) {
  return request({ url: '/oa/workflow/definition/list', method: 'get', params: query })
}

// 获取流程图XML
export function getDefinitionXml(definitionId) {
  return request({ url: '/oa/workflow/definition/' + definitionId + '/xml', method: 'get' })
}

// 删除部署
export function delDeployment(deploymentId) {
  return request({ url: '/oa/workflow/definition/delete/' + deploymentId, method: 'post' })
}

// 查询流程实例列表
export function listInstance(query) {
  return request({ url: '/oa/workflow/instance/list', method: 'get', params: query })
}

// 查询待办任务
export function listTodoTask(query) {
  return request({ url: '/oa/workflow/task/todo', method: 'get', params: query })
}

// 完成任务
export function completeTask(taskId, data) {
  return request({ url: '/oa/workflow/task/complete/' + taskId, method: 'post', data: data })
}

// 驳回任务
export function rejectTask(taskId, data) {
  return request({ url: '/oa/workflow/task/reject/' + taskId, method: 'post', data: data })
}

// 转办任务
export function transferTask(taskId, data) {
  return request({ url: '/oa/workflow/task/transfer/' + taskId, method: 'post', data: data })
}
