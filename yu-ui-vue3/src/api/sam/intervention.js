import request from '@/utils/request'

// ===== F4 学生干预闭环台账（预警→帮扶→效果回收）——对应后端 SamStudentInterventionController /sam/intervention =====

// 查询干预台账（GET /sam/intervention/list）
export function listIntervention(query) {
  return request({ url: '/sam/intervention/list', method: 'get', params: query })
}

// 查询单条干预记录（GET /sam/intervention/{interventionId}）
export function getIntervention(interventionId) {
  return request({ url: '/sam/intervention/' + interventionId, method: 'get' })
}

// 登记干预记录（POST /sam/intervention）
export function addIntervention(data) {
  return request({ url: '/sam/intervention', method: 'post', data: data })
}

// 修改干预记录（PUT /sam/intervention）
export function updateIntervention(data) {
  return request({ url: '/sam/intervention', method: 'put', data: data })
}

// 批量删除干预记录（DELETE /sam/intervention/{interventionIds}）
export function delIntervention(interventionIds) {
  return request({ url: '/sam/intervention/' + interventionIds, method: 'delete' })
}

// 效果预览：按基线/效果学期现算 GPA 与及格率变化（GET /sam/intervention/preview）
export function previewInterventionEffect(query) {
  return request({ url: '/sam/intervention/preview', method: 'get', params: query })
}
