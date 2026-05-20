import request from '@/utils/request'

// 查询考试安排（学生端）
export function listExam(query) {
  return request({ url: '/portal/exam/list', method: 'get', params: query })
}

// 查询监考安排（教师端）
export function listInvigilation(query) {
  return request({ url: '/portal/exam/invigilationList', method: 'get', params: query })
}
