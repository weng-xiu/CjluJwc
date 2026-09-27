import request from '@/utils/request'

// 查询教学任务列表（教师端）
export function listTeachingTask(query) {
  return request({ url: '/portal/teachingTask/list', method: 'get', params: query })
}
