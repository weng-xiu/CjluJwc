import request from '@/utils/request'

// 查询成绩权重配置列表
export function listGradeWeight(query) {
  return request({ url: '/aem/gradeWeight/list', method: 'get', params: query })
}

// 查询成绩权重配置详细
export function getGradeWeight(weightId) {
  return request({ url: '/aem/gradeWeight/' + weightId, method: 'get' })
}

// 新增成绩权重配置
export function addGradeWeight(data) {
  return request({ url: '/aem/gradeWeight', method: 'post', data: data })
}

// 修改成绩权重配置
export function updateGradeWeight(data) {
  return request({ url: '/aem/gradeWeight', method: 'put', data: data })
}

// 删除成绩权重配置
export function delGradeWeight(weightId) {
  return request({ url: '/aem/gradeWeight/' + weightId, method: 'delete' })
}

// 预览课程生效权重
export function effectiveWeight(courseId) {
  return request({ url: '/aem/gradeWeight/effective/' + courseId, method: 'get' })
}
