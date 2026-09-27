import request from '@/utils/request'

// 查询离校环节配置列表
export function listProcedureStep(query) {
  return request({ url: '/sam/procedureStep/list', method: 'get', params: query })
}

// 全部启用环节（不分页，供办理对话框加载）
export function allProcedureStep() {
  return request({ url: '/sam/procedureStep/all', method: 'get' })
}

// 查询环节详细
export function getProcedureStep(stepId) {
  return request({ url: '/sam/procedureStep/' + stepId, method: 'get' })
}

// 新增环节
export function addProcedureStep(data) {
  return request({ url: '/sam/procedureStep', method: 'post', data: data })
}

// 修改环节
export function updateProcedureStep(data) {
  return request({ url: '/sam/procedureStep', method: 'put', data: data })
}

// 删除环节
export function delProcedureStep(stepIds) {
  return request({ url: '/sam/procedureStep/' + stepIds, method: 'delete' })
}
