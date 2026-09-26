import request from '@/utils/request'

// 监考安排列表（移动端，支持分页及 dutyType 等查询参数）
// 走门户权限端点，教师角色仅需 portal:invigilation:list
export function listInvigilations(query) {
  return request({ url: '/portal/exam/invigilationList', method: 'get', params: query })
}
