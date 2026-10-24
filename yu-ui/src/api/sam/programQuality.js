import request from '@/utils/request'

// ===== F3 专业质量档案一页纸（只读聚合）——对应后端 SamProgramQualityController /sam/programQuality =====

// 可选专业清单（专业下拉）
export function listQualityMajors() {
  return request({ url: '/sam/programQuality/majors', method: 'get' })
}

// 生成指定专业的质量档案一页纸
export function getProgramProfile(majorId) {
  return request({ url: '/sam/programQuality/' + majorId, method: 'get' })
}
