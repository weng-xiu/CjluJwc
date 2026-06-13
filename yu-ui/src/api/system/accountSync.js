import request from '@/utils/request'

// 同步教师账号
export function syncTeachers(deptId) {
  return request({
    url: '/system/account/sync/teachers',
    method: 'post',
    params: { deptId: deptId }
  })
}

// 同步学生账号
export function syncStudents(classId) {
  return request({
    url: '/system/account/sync/students',
    method: 'post',
    params: { classId: classId }
  })
}
