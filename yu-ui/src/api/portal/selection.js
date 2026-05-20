import request from '@/utils/request'

// 查询选课轮次
export function listRound(query) {
  return request({ url: '/portal/selection/roundList', method: 'get', params: query })
}

// 查询可选课程列表
export function listCourse(query) {
  return request({ url: '/portal/selection/courseList', method: 'get', params: query })
}

// 选课操作
export function enroll(data) {
  return request({ url: '/portal/selection/enroll', method: 'post', data: data })
}
