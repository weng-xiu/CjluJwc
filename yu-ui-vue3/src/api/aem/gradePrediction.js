import request from '@/utils/request'

// 学生学业风险预测（趋势拟合 + 分级 + 建议）
export function studentRisk(studentId) {
  return request({ url: '/aem/gradePrediction/studentRisk', method: 'get', params: { studentId } })
}

// 课程难度画像（semesterId 可选）
export function courseDifficulty(semesterId) {
  return request({ url: '/aem/gradePrediction/courseDifficulty', method: 'get', params: { semesterId } })
}

// 班级/学期学业风险看板（返回学生风险明细 + 分布统计）
export function riskBoard(semesterId, classId) {
  return request({ url: '/aem/gradePrediction/riskBoard', method: 'get', params: { semesterId, classId } })
}
