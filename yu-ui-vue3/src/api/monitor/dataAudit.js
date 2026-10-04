import request from '@/utils/request'

// 查询字段级数据变更流水列表（K1 合规③，只读）
export function listDataAudit(query) {
  return request({
    url: '/monitor/dataAudit/list',
    method: 'get',
    params: query
  })
}

// 查询某条业务记录（实体类型 + 业务主键）的字段变更历史
export function getBizHistory(entityType, bizId) {
  return request({
    url: '/monitor/dataAudit/biz/' + entityType + '/' + bizId,
    method: 'get'
  })
}
