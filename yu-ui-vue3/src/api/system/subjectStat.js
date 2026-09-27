import request from '@/utils/request'

// 学生结构主题
export function studentStructure() {
  return request({ url: '/system/subjectStat/studentStructure', method: 'get' })
}

// 成绩分析主题（学期可选）
export function gradeAnalysis(query) {
  return request({ url: '/system/subjectStat/gradeAnalysis', method: 'get', params: query })
}

// 师资分析主题（学期可选）
export function teacherStructure(query) {
  return request({ url: '/system/subjectStat/teacherStructure', method: 'get', params: query })
}
