import request from '@/utils/request'

// 查询成绩统计列表
export function listGradeStatistics(query) {
  return request({ url: '/aem/gradeStatistics/list', method: 'get', params: query })
}

// 查询成绩统计详细
export function getGradeStatistics(statId) {
  return request({ url: '/aem/gradeStatistics/' + statId, method: 'get' })
}

// 按课程+学期聚合统计
export function aggregateGrade(courseId, semesterId) {
  return request({ url: '/aem/gradeStatistics/aggregate', method: 'post', params: { courseId, semesterId } })
}

// 按学期批量聚合
export function aggregateSemester(semesterId) {
  return request({ url: '/aem/gradeStatistics/aggregateSemester', method: 'post', params: { semesterId } })
}

// 分数段分布
export function scoreDistribution(courseId, semesterId) {
  return request({ url: '/aem/gradeStatistics/distribution', method: 'get', params: { courseId, semesterId } })
}

// 学期总览
export function semesterOverview(semesterId) {
  return request({ url: '/aem/gradeStatistics/overview', method: 'get', params: { semesterId } })
}

// 课程成绩排名
export function courseRanking(query) {
  return request({ url: '/aem/gradeStatistics/ranking', method: 'get', params: query })
}
