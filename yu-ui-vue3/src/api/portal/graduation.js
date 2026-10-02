import request from '@/utils/request'

// 学生本人毕业资格自助预审（只读试算，不落库；后端强制绑定当前登录用户）
export function getGraduationPreReview() {
  return request({ url: '/portal/graduation/preReview', method: 'get' })
}
