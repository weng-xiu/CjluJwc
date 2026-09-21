import request from '@/utils/request'

// 评教统计总览（核心指标 + 分数段分布）
export function evalOverview(query) {
  return request({ url: '/aem/evaluationStat/overview', method: 'get', params: query })
}

// 按课程聚合
export function evalByCourse(query) {
  return request({ url: '/aem/evaluationStat/byCourse', method: 'get', params: query })
}

// 按教师聚合（含排名）
export function evalByTeacher(query) {
  return request({ url: '/aem/evaluationStat/byTeacher', method: 'get', params: query })
}

// 按班级聚合
export function evalByClass(query) {
  return request({ url: '/aem/evaluationStat/byClass', method: 'get', params: query })
}

// 月度趋势
export function evalTrend(query) {
  return request({ url: '/aem/evaluationStat/trend', method: 'get', params: query })
}

// 评语词频分析
export function evalCommentAnalysis(query) {
  return request({ url: '/aem/evaluationStat/commentAnalysis', method: 'get', params: query })
}
