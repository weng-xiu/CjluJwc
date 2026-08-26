import request from '@/utils/request'

// 监考安排列表（移动端，支持分页及查询参数）
export function listInvigilations(query) {
  return request({ url: '/aem/invigilation/list', method: 'get', params: query })
}
