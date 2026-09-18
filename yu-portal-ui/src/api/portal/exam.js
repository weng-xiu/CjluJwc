import request from '@/utils/request'

export function listExam(query) {
  return request({ url: '/portal/exam/list', method: 'get', params: query })
}

export function listInvigilation(query) {
  return request({ url: '/portal/exam/invigilationList', method: 'get', params: query })
}

// 考试安排列表（移动端，支持 examName/examType/planStatus 等查询参数及分页）
// 走门户权限端点，学生角色仅需 portal:exam:list
export function listExams(query) {
  return request({ url: '/portal/exam/list', method: 'get', params: query })
}
