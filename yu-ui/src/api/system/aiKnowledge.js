import request from '@/utils/request'

// 查询AI知识库条目列表
export function listAiKnowledge(query) {
  return request({ url: '/system/aiKnowledge/list', method: 'get', params: query })
}

// 查询AI知识库条目详细
export function getAiKnowledge(knowledgeId) {
  return request({ url: '/system/aiKnowledge/' + knowledgeId, method: 'get' })
}

// 新增AI知识库条目
export function addAiKnowledge(data) {
  return request({ url: '/system/aiKnowledge', method: 'post', data: data })
}

// 修改AI知识库条目
export function updateAiKnowledge(data) {
  return request({ url: '/system/aiKnowledge', method: 'put', data: data })
}

// 删除AI知识库条目
export function delAiKnowledge(knowledgeIds) {
  return request({ url: '/system/aiKnowledge/' + knowledgeIds, method: 'delete' })
}

// 知识库治理统计（分类分布 + 热门条目）
export function getAiKnowledgeStat() {
  return request({ url: '/system/aiKnowledge/stat', method: 'get' })
}

// 回答引擎状态（大模型是否接入、检索阈值、启用条目数）
export function getAiEngine() {
  return request({ url: '/system/aiKnowledge/engine', method: 'get' })
}

// 后台自测问答
export function askAiKnowledge(question) {
  return request({ url: '/system/aiKnowledge/ask', method: 'post', data: { question: question } })
}

// 推荐问法（取自知识库真实条目）
export function suggestAiQuestion(limit) {
  return request({ url: '/system/aiKnowledge/suggest', method: 'get', params: { limit: limit } })
}
