import request from '@/utils/request'

// 查询学年列表
export function listYear(query) {
  return request({ url: '/brm/year/list', method: 'get', params: query })
}

// 查询学年详细
export function getYear(yearId) {
  return request({ url: '/brm/year/' + yearId, method: 'get' })
}

// 新增学年
export function addYear(data) {
  return request({ url: '/brm/year', method: 'post', data: data })
}

// 修改学年
export function updateYear(data) {
  return request({ url: '/brm/year', method: 'put', data: data })
}

// 删除学年
export function delYear(yearId) {
  return request({ url: '/brm/year/' + yearId, method: 'delete' })
}
