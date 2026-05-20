import request from '@/utils/request'

export function listTeachingTasks(query) {
  return request({ url: '/portal/teachingTask/list', method: 'get', params: query })
}
