import request from '@/utils/request'

// S4：学生本人毕业资格自助预审（只读）
export function preReview() {
  return request({ url: '/portal/graduation/preReview', method: 'get' })
}
