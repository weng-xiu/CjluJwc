import request from '@/utils/request'

// 教务政策智能问答（scene 由后端固定为 portal，用户身份取自登录态）
export function askAi(question) {
  return request({ url: '/portal/ai/ask', method: 'post', data: { question } })
}

// 推荐问法（取自知识库真实条目）
export function suggestAi(limit) {
  return request({ url: '/portal/ai/suggest', method: 'get', params: { limit } })
}

// 当前回答引擎说明（披露是否已接入大模型）
export function getAiEngine() {
  return request({ url: '/portal/ai/engine', method: 'get' })
}

// 个性化选课推荐
export function recommendCourses() {
  return request({ url: '/portal/ai/recommend', method: 'get' })
}

// 学业画像
export function getPortrait() {
  return request({ url: '/portal/ai/portrait', method: 'get' })
}
