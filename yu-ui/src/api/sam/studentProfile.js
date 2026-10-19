import request from '@/utils/request'

// ===== N6 学生全息数字档案（只读聚合）——对应后端 SamStudentProfileController /sam/studentProfile =====

// 生成指定学生的全息数字档案（GET /sam/studentProfile/{studentId}）
export function getStudentProfile(studentId) {
  return request({ url: '/sam/studentProfile/' + studentId, method: 'get' })
}
