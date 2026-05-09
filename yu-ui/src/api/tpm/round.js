import request from '@/utils/request'

// 查询选课轮次列表
export function listRound(query) {
  return request({ url: '/tpm/round/list', method: 'get', params: query })
}

// 查询选课轮次详细
export function getRound(roundId) {
  return request({ url: '/tpm/round/' + roundId, method: 'get' })
}

// 新增选课轮次
export function addRound(data) {
  return request({ url: '/tpm/round', method: 'post', data: data })
}

// 修改选课轮次
export function updateRound(data) {
  return request({ url: '/tpm/round', method: 'put', data: data })
}

// 删除选课轮次
export function delRound(roundId) {
  return request({ url: '/tpm/round/' + roundId, method: 'delete' })
}
