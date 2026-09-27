import request from '@/utils/request'

// 查询AI问答留痕列表
export function listAiChat(query) {
  return request({ url: '/system/aiChat/list', method: 'get', params: query })
}

// 查询单次问答留痕详情（含完整回答与引用条目）
export function getAiChat(recordId) {
  return request({ url: '/system/aiChat/' + recordId, method: 'get' })
}

// 问答效果统计（总览、来源分布、场景分布、按日趋势、未命中问题、热门条目）
export function getAiChatStat(query) {
  return request({ url: '/system/aiChat/stat', method: 'get', params: query })
}
