import request from '@/utils/request'

// 部署流程文件
export function deployProcess(data) {
  return request({ url: '/oa/workflow/deploy', method: 'post', data: data })
}

// 查询流程定义列表
export function listDefinition(query) {
  return request({ url: '/oa/workflow/definition/list', method: 'get', params: query })
}

// 获取流程图XML（definitionId含特殊字符，用params传递）
export function getDefinitionXml(definitionId) {
  return request({ url: '/oa/workflow/definition/xml', method: 'get', params: { definitionId } })
}

// 新增流程定义（BPMN XML字符串）
export function createDefinition(data) {
  return request({ url: '/oa/workflow/definition/create', method: 'post', data: data })
}
// 删除部署
export function delDeployment(deploymentId) {
  return request({ url: '/oa/workflow/definition/delete/' + deploymentId, method: 'post' })
}

// 查询流程实例列表
export function listInstance(query) {
  return request({ url: '/oa/workflow/instance/list', method: 'get', params: query })
}

// 获取流程实例详情（含历史任务与审批意见）
export function getInstanceDetail(processInstanceId) {
  return request({ url: '/oa/workflow/instance/detail', method: 'get', params: { processInstanceId } })
}

// 终止运行中的流程实例
export function cancelInstance(data) {
  return request({ url: '/oa/workflow/instance/cancel', method: 'post', data: data })
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

// 获取流程图渲染数据（节点/连线/进度）
export function getProcessDiagram(processInstanceId) {
  return request({ url: '/oa/workflow/diagram', method: 'get', params: { processInstanceId } })
}

// 加签（mode：0前加签 1后加签）
export function addSignTask(taskId, data) {
  return request({ url: '/oa/workflow/task/addSign/' + taskId, method: 'post', data: data })
}

// 会签（rule：ALL全部同意 ANY一人同意即定论）
export function counterSignTask(taskId, data) {
  return request({ url: '/oa/workflow/task/counterSign/' + taskId, method: 'post', data: data })
}

// 委托代办
export function delegateTask(taskId, data) {
  return request({ url: '/oa/workflow/task/delegate/' + taskId, method: 'post', data: data })
}

// 收回委托
export function reclaimDelegateTask(taskId, data) {
  return request({ url: '/oa/workflow/task/delegate/reclaim/' + taskId, method: 'post', data: data })
}

// 提交加签/会签意见
export function submitOpinion(itemId, data) {
  return request({ url: '/oa/workflow/task/opinion/' + itemId, method: 'post', data: data })
}

// 我的加签/会签待办
export function listMyOpinion(query) {
  return request({ url: '/oa/workflow/task/opinion/my', method: 'get', params: query })
}

// 协同留痕（按流程实例）
export function listOpinionByInstance(processInstanceId) {
  return request({ url: '/oa/workflow/task/opinion/list', method: 'get', params: { processInstanceId } })
}

// 可选协同办理人
export function listCoSignUsers() {
  return request({ url: '/oa/workflow/task/opinion/users', method: 'get' })
}
