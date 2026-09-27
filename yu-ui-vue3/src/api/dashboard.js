import request from '@/utils/request'

// 教务数据驾驶舱总览（P2）
export function getDashboardOverview() {
  return request({
    url: '/system/dashboard/overview',
    method: 'get'
  })
}
