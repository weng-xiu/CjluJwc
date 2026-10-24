import request from '@/utils/request'

// 查询质量异常工单列表
export function listQualityTicket(query) {
  return request({ url: '/dis/qualityTicket/list', method: 'get', params: query })
}

// 查询质量异常工单详细
export function getQualityTicket(ticketId) {
  return request({ url: '/dis/qualityTicket/' + ticketId, method: 'get' })
}

// 登记质量异常工单
export function addQualityTicket(data) {
  return request({ url: '/dis/qualityTicket', method: 'post', data: data })
}

// 处置质量异常工单（指派/推进状态/回填结论）
export function updateQualityTicket(data) {
  return request({ url: '/dis/qualityTicket', method: 'put', data: data })
}

// 删除质量异常工单
export function delQualityTicket(ticketId) {
  return request({ url: '/dis/qualityTicket/' + ticketId, method: 'delete' })
}

// 异常检测：扫描同步任务失败率超标自动生成待处理工单（幂等）
export function detectAnomaly(threshold) {
  return request({ url: '/dis/qualityTicket/detect', method: 'post', params: { threshold: threshold } })
}
