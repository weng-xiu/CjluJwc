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

// 开启选课轮次
export function startRound(roundId) {
  return request({ url: '/tpm/round/start/' + roundId, method: 'put' })
}

// 结束选课轮次
export function finishRound(roundId) {
  return request({ url: '/tpm/round/finish/' + roundId, method: 'put' })
}
