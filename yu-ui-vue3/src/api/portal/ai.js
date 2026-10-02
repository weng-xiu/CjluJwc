import request from '@/utils/request'

// 教务政策智能问答（返回 AiAnswer：answer/answerSource/confidence/references/engineNote 等）
export function aiAsk(question) {
  return request({ url: '/portal/ai/ask', method: 'post', data: { question } })
}

// 推荐问法（返回字符串数组）
export function aiSuggest(limit) {
  return request({ url: '/portal/ai/suggest', method: 'get', params: { limit } })
}

// 当前回答引擎说明（{llmEnabled, engineNote}）
export function aiEngine() {
  return request({ url: '/portal/ai/engine', method: 'get' })
}

// 个性化选课推荐（仅学生/管理员）
export function aiRecommend() {
  return request({ url: '/portal/ai/recommend', method: 'get' })
}

// 学业画像（六维评分 + 模块达成 + 建议，仅学生/管理员）
export function aiPortrait() {
  return request({ url: '/portal/ai/portrait', method: 'get' })
}
